import SwiftUI

/// Full list of supported interface languages with live search.
struct LanguagePickerView: View {
    @Environment(AppStore.self) private var store

    @State private var query: String = ""

    private var results: [AppLanguage] {
        let trimmed = query.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else { return AppLanguage.all }
        return AppLanguage.all.filter {
            $0.englishName.localizedCaseInsensitiveContains(trimmed)
                || $0.nativeName.localizedCaseInsensitiveContains(trimmed)
                || $0.code.localizedCaseInsensitiveContains(trimmed)
        }
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                searchField

                if results.isEmpty {
                    Text("No language matches “\(query)”")
                        .font(.system(size: 14))
                        .foregroundStyle(Theme.inkFaint)
                        .padding(.top, 40)
                } else {
                    VStack(spacing: 0) {
                        ForEach(Array(results.enumerated()), id: \.element.id) { index, language in
                            if index > 0 {
                                Divider().overlay(Theme.hairline).padding(.leading, 56)
                            }
                            row(for: language)
                        }
                    }
                    .cardStyle(radius: 22, padding: 0)
                    .padding(.horizontal, 20)
                }

                Text(L("l.note"))
                    .font(.system(size: 12))
                    .foregroundStyle(Theme.inkFaint)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.horizontal, 34)
                    .padding(.top, 2)
            }
            .padding(.top, 6)
            .padding(.bottom, 40)
        }
        .scrollIndicators(.hidden)
        .scrollDismissesKeyboard(.immediately)
        .background(Theme.backdrop)
        .navigationTitle(L("l.title"))
        .navigationBarTitleDisplayMode(.inline)
    }

    private var searchField: some View {
        HStack(spacing: 9) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(Theme.inkFaint)
            TextField(L("l.search"), text: $query)
                .font(.system(size: 16))
                .foregroundStyle(Theme.ink)
                .autocorrectionDisabled()
                .textInputAutocapitalization(.never)
            if !query.isEmpty {
                Button {
                    Haptics.selection()
                    query = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 15))
                        .foregroundStyle(Theme.inkFaint)
                }
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 13)
        .cardStyle(radius: 18, padding: 0)
        .padding(.horizontal, 20)
    }

    private func row(for language: AppLanguage) -> some View {
        let isSelected = language.code == store.language.code

        return Button {
            guard !isSelected else { return }
            Haptics.success()
            withAnimation(.spring(response: 0.35, dampingFraction: 0.9)) {
                store.language = language
            }
        } label: {
            HStack(spacing: 13) {
                Text(language.flag)
                    .font(.system(size: 25))
                    .frame(width: 32)

                VStack(alignment: .leading, spacing: 2) {
                    Text(language.nativeName)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(Theme.ink)
                    Text(language.englishName)
                        .font(.system(size: 12))
                        .foregroundStyle(Theme.inkFaint)
                }
                .environment(\.layoutDirection, .leftToRight)

                Spacer(minLength: 8)

                if isSelected {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 19))
                        .foregroundStyle(Theme.mint)
                        .transition(.scale.combined(with: .opacity))
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 13)
            .contentShape(.rect)
        }
        .buttonStyle(PressableButtonStyle())
        // Each row previews its own script, so keep names in their natural direction.
        .environment(\.layoutDirection, .leftToRight)
    }
}

#Preview {
    NavigationStack {
        LanguagePickerView()
            .environment(AppStore())
    }
}
