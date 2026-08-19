import SwiftUI

/// One logged meal in the home timeline.
///
/// The card holds two separate hit targets: the body opens the meal detail, and
/// the trailing pencil jumps straight to the quick-edit form.
struct MealRow: View {
    let meal: MealEntry
    let thumbnail: UIImage?
    let onOpen: () -> Void
    let onEdit: () -> Void

    var body: some View {
        HStack(spacing: 8) {
            Button {
                Haptics.tap()
                onOpen()
            } label: {
                rowContent
            }
            .buttonStyle(PressableButtonStyle())
            .accessibilityLabel("\(meal.title), \(meal.calories) calories")
            .accessibilityHint("Opens meal details")

            Button {
                Haptics.tap()
                onEdit()
            } label: {
                Image(systemName: "pencil")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(Theme.inkSoft)
                    .frame(width: 34, height: 34)
                    .background(Theme.well, in: Circle())
            }
            .buttonStyle(PressableButtonStyle())
            .accessibilityLabel("Edit \(meal.title)")
        }
        .padding(12)
        .cardStyle(radius: 22, padding: 0)
    }

    private var rowContent: some View {
        HStack(spacing: 13) {
            ZStack {
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(Theme.well)
                if let thumbnail {
                    Image(uiImage: thumbnail)
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .allowsHitTesting(false)
                } else {
                    Image(systemName: meal.source.symbol)
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundStyle(Theme.inkFaint)
                }
            }
            .frame(width: 58, height: 58)
            .clipShape(.rect(cornerRadius: 16, style: .continuous))

            VStack(alignment: .leading, spacing: 5) {
                Text(meal.title)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(Theme.ink)
                    .lineLimit(1)

                HStack(spacing: 8) {
                    Label {
                        Text("\(meal.calories)")
                            .font(.system(size: 13, weight: .bold))
                            .contentTransition(.numericText(value: Double(meal.calories)))
                    } icon: {
                        Image(systemName: "flame.fill").font(.system(size: 11))
                    }
                    .foregroundStyle(Theme.flame)

                    macroChip("\(Int(meal.protein))P", Theme.protein)
                    macroChip("\(Int(meal.carbs))C", Theme.carbs)
                    macroChip("\(Int(meal.fat))F", Theme.fat)
                }
            }

            Spacer(minLength: 0)

            HStack(spacing: 4) {
                Image(systemName: meal.slot.symbol)
                    .font(.system(size: 10))
                Text(meal.date.formatted(date: .omitted, time: .shortened))
                    .font(.system(size: 12, weight: .medium))
            }
            .foregroundStyle(Theme.inkFaint)
            .layoutPriority(1)
        }
        .contentShape(Rectangle())
    }

    private func macroChip(_ text: String, _ color: Color) -> some View {
        Text(text)
            .font(.system(size: 11, weight: .semibold))
            .foregroundStyle(color)
            .padding(.horizontal, 7)
            .padding(.vertical, 3)
            .background(color.opacity(0.12), in: Capsule())
    }
}
