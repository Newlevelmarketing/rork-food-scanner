import Foundation
import ImageIO
import UIKit
import UniformTypeIdentifiers

nonisolated enum NutritionAIError: LocalizedError {
    case notConfigured
    case offline
    case timedOut
    case imageTooLarge
    case authError
    case insufficientBalance
    case rateLimited
    case notFood
    case badResponse
    case serverError(Int)

    var errorDescription: String? {
        switch self {
        case .notConfigured: "Meal analysis is unavailable right now. You can still add a meal by searching the food database."
        case .offline: "You appear to be offline. Connect to the internet and try again, or search the food database instead."
        case .timedOut: "That took too long to analyse. Check your connection and try again."
        case .imageTooLarge: "That photo is too large. Try taking a new one."
        case .authError: "Meal analysis is temporarily unavailable. Please try again later."
        case .insufficientBalance: "Meal analysis is temporarily unavailable. Please try again later."
        case .rateLimited: "Too many scans at once. Wait a moment and try again."
        case .notFood: "We couldn't find any food in that photo. Try again with better lighting, or describe the meal instead."
        case .badResponse: "We couldn't read that result. Please try again."
        case .serverError: "Something went wrong on our side. Please try again in a moment."
        }
    }

    /// Maps transport-level failures onto user-facing cases.
    static func from(_ error: Error) -> NutritionAIError {
        if let known = error as? NutritionAIError { return known }
        let nsError = error as NSError
        guard nsError.domain == NSURLErrorDomain else { return .badResponse }
        switch nsError.code {
        case NSURLErrorNotConnectedToInternet,
             NSURLErrorNetworkConnectionLost,
             NSURLErrorDataNotAllowed,
             NSURLErrorCannotConnectToHost,
             NSURLErrorCannotFindHost:
            return .offline
        case NSURLErrorTimedOut:
            return .timedOut
        default:
            return .badResponse
        }
    }
}

/// Structured nutrition estimate returned by the model.
nonisolated struct AnalysisResult: Codable, Equatable {
    struct Item: Codable, Equatable {
        let name: String
        let quantity: String
        let calories: Int
        let protein: Double
        let carbs: Double
        let fat: Double
    }

    let title: String
    let isFood: Bool
    let healthScore: Int
    let items: [Item]
    let quip: String?

    var foodItems: [FoodItem] {
        items.map {
            FoodItem(
                name: $0.name,
                quantity: $0.quantity,
                calories: max(0, $0.calories),
                protein: max(0, $0.protein),
                carbs: max(0, $0.carbs),
                fat: max(0, $0.fat)
            )
        }
    }
}

/// Talks to the Rork Toolkit proxy (Vercel AI Gateway) for meal recognition.
nonisolated struct NutritionAI {
    private static let model = "google/gemini-3-flash"
    private static let fallbackModels = ["anthropic/claude-haiku-4.5", "openai/gpt-5-mini"]

    private static var endpoint: URL? {
        let base = Config.EXPO_PUBLIC_TOOLKIT_URL.trimmingCharacters(in: .whitespaces)
        guard !base.isEmpty else { return nil }
        let normalized = base.hasSuffix("/") ? String(base.dropLast()) : base
        return URL(string: "\(normalized)/v2/vercel/v1/chat/completions")
    }

    static var isConfigured: Bool { endpoint != nil }

    private static func systemPrompt(jester: Bool, language: String) -> String {
        var prompt = """
        You are ModernBody, a precise nutrition estimator. Given a meal photo or description, \
        identify each distinct food component and estimate its nutrition for the portion shown.

        Rules:
        - Estimate realistic portion sizes from visual cues (plate size, utensils, hands).
        - Break composite dishes into their main components when clearly separable, \
        otherwise return the dish as one item.
        - Quantities must be human readable, e.g. "1 medium bowl", "150 g", "2 slices".
        - healthScore is 1-10 where 10 is an exceptionally nutritious, whole-food meal.
        - title is a short, appetising name for the whole meal (max 4 words).
        - If the input clearly contains no edible food, set isFood to false and return an empty items array.
        - Never comment on the user's body, weight, appearance or self-worth, and never \
        frame food as a moral failing. Do not give medical, diagnostic or clinical dietary advice.

        Respond with ONLY raw JSON matching exactly this shape, no markdown fences:
        {"title":string,"isFood":boolean,"healthScore":number,"items":[{"name":string,\
        "quantity":string,"calories":number,"protein":number,"carbs":number,"fat":number}],"quip":string}

        Write title, every item name, quantity and quip in \(language). Keep the JSON keys in English.
        """
        if jester {
            prompt += """


            Set quip to one playful, self-deprecating one-line joke about the food itself \
            (max 14 words). Roast the meal, never the person. No insults about the user's \
            body, weight or discipline, and nothing shaming.
            """
        } else {
            prompt += "\n\nSet quip to one short, warm, encouraging note about this meal (max 12 words)."
        }
        return prompt
    }

    // MARK: - Public API

    static func analyze(
        image: UIImage,
        jesterMode: Bool,
        language: String = "English"
    ) async throws -> AnalysisResult {
        guard let base64 = downscaledBase64(from: image) else { throw NutritionAIError.imageTooLarge }
        let content: [[String: Any]] = [
            ["type": "text", "text": "Analyse this meal photo and return the JSON."],
            ["type": "image_url", "image_url": ["url": "data:image/jpeg;base64,\(base64)"]]
        ]
        return try await send(userContent: content, jesterMode: jesterMode, language: language)
    }

    static func analyze(
        text: String,
        jesterMode: Bool,
        language: String = "English"
    ) async throws -> AnalysisResult {
        let content: [[String: Any]] = [
            ["type": "text", "text": "Meal description: \"\(text)\". Return the JSON."]
        ]
        return try await send(userContent: content, jesterMode: jesterMode, language: language)
    }

    // MARK: - Request

    private static func send(
        userContent: [[String: Any]],
        jesterMode: Bool,
        language: String
    ) async throws -> AnalysisResult {
        guard let endpoint else { throw NutritionAIError.notConfigured }

        let body: [String: Any] = [
            "model": model,
            "temperature": 0.2,
            "messages": [
                ["role": "system", "content": systemPrompt(jester: jesterMode, language: language)],
                ["role": "user", "content": userContent]
            ],
            "providerOptions": ["gateway": ["models": fallbackModels]]
        ]

        var request = URLRequest(url: endpoint)
        request.httpMethod = "POST"
        request.timeoutInterval = 60
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(
            "Bearer \(Config.EXPO_PUBLIC_RORK_TOOLKIT_SECRET_KEY)",
            forHTTPHeaderField: "Authorization"
        )
        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await URLSession.shared.data(for: request)
        } catch {
            throw NutritionAIError.from(error)
        }
        guard let http = response as? HTTPURLResponse else { throw NutritionAIError.badResponse }

        switch http.statusCode {
        case 200: break
        case 401, 403: throw NutritionAIError.authError
        case 402: throw NutritionAIError.insufficientBalance
        case 413: throw NutritionAIError.imageTooLarge
        case 429: throw NutritionAIError.rateLimited
        case 408, 504: throw NutritionAIError.timedOut
        default:
            print("[NutritionAI] request failed with status \(http.statusCode)")
            throw NutritionAIError.serverError(http.statusCode)
        }

        struct ChatResponse: Decodable {
            struct Choice: Decodable {
                struct Message: Decodable { let content: String? }
                let message: Message
            }
            let choices: [Choice]
        }

        guard
            let decoded = try? JSONDecoder().decode(ChatResponse.self, from: data),
            let text = decoded.choices.first?.message.content,
            let json = extractJSON(from: text),
            let payload = json.data(using: .utf8)
        else { throw NutritionAIError.badResponse }

        guard let result = try? JSONDecoder().decode(AnalysisResult.self, from: payload) else {
            throw NutritionAIError.badResponse
        }
        guard result.isFood, !result.items.isEmpty else { throw NutritionAIError.notFood }
        return result
    }

    /// Pulls the first balanced JSON object out of a possibly fenced model reply.
    private static func extractJSON(from text: String) -> String? {
        guard let start = text.firstIndex(of: "{") else { return nil }
        var depth = 0
        var index = start
        while index < text.endIndex {
            let character = text[index]
            if character == "{" { depth += 1 }
            if character == "}" {
                depth -= 1
                if depth == 0 {
                    return String(text[start...index])
                }
            }
            index = text.index(after: index)
        }
        return nil
    }

    // MARK: - Image budget

    /// Walks a resize/quality ladder until the JPEG fits inside the gateway payload budget.
    private static func downscaledBase64(from image: UIImage, maxBytes: Int = 2_600_000) -> String? {
        let ladder: [(CGFloat, CGFloat)] = [(1280, 0.82), (1024, 0.78), (832, 0.74), (640, 0.70), (512, 0.65)]
        for (maxEdge, quality) in ladder {
            guard let resized = resize(image, maxEdge: maxEdge),
                  let jpeg = resized.jpegData(compressionQuality: quality) else { continue }
            if jpeg.count <= maxBytes {
                return jpeg.base64EncodedString()
            }
        }
        return nil
    }

    private static func resize(_ image: UIImage, maxEdge: CGFloat) -> UIImage? {
        let longest = max(image.size.width, image.size.height)
        guard longest > 0 else { return nil }
        let scale = min(1, maxEdge / longest)
        let target = CGSize(width: image.size.width * scale, height: image.size.height * scale)
        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 1
        format.opaque = true
        return UIGraphicsImageRenderer(size: target, format: format).image { _ in
            image.draw(in: CGRect(origin: .zero, size: target))
        }
    }
}
