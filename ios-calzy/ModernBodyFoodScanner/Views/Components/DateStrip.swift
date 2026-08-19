import SwiftUI

/// Horizontal week selector pinned under the app title.
struct DateStrip: View {
    @Binding var selected: Date
    let hasLogs: (Date) -> Bool

    private let calendar = Calendar.current

    private var days: [Date] {
        let today = calendar.startOfDay(for: Date())
        return (0..<21).reversed().compactMap {
            calendar.date(byAdding: .day, value: -$0, to: today)
        }
    }

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(days, id: \.timeIntervalSince1970) { day in
                        dayCell(day)
                            .id(day.timeIntervalSince1970)
                    }
                }
                .padding(.vertical, 4)
            }
            .contentMargins(.horizontal, 20, for: .scrollContent)
            .onAppear {
                proxy.scrollTo(days.last?.timeIntervalSince1970, anchor: .trailing)
            }
        }
    }

    private func dayCell(_ day: Date) -> some View {
        let isSelected = calendar.isDate(day, inSameDayAs: selected)
        let isToday = calendar.isDateInToday(day)

        return Button {
            Haptics.selection()
            withAnimation(.spring(response: 0.35, dampingFraction: 0.78)) {
                selected = day
            }
        } label: {
            VStack(spacing: 6) {
                Text(day.formatted(.dateTime.weekday(.abbreviated)).uppercased())
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(isSelected ? Theme.ink : Theme.inkFaint)

                ZStack {
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .fill(isSelected ? Color.white : Color.clear)
                        .shadow(color: .black.opacity(isSelected ? 0.08 : 0), radius: 10, y: 4)

                    Text("\(calendar.component(.day, from: day))")
                        .font(.metric(19, isSelected ? .bold : .medium))
                        .foregroundStyle(isSelected ? Theme.ink : Theme.inkFaint)
                }
                .frame(width: 46, height: 50)
                .overlay(alignment: .topTrailing) {
                    if isToday && !isSelected {
                        Circle()
                            .fill(Theme.flame)
                            .frame(width: 5, height: 5)
                            .padding(6)
                    }
                }

                Circle()
                    .fill(hasLogs(day) ? Theme.mint : .clear)
                    .frame(width: 5, height: 5)
            }
        }
        .buttonStyle(PressableButtonStyle())
    }
}
