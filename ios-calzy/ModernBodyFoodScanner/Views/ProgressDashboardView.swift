import Charts
import PhotosUI
import SwiftUI

/// Progress tab: weight, BMI, streaks, weight journal, trend chart and photos.
struct ProgressDashboardView: View {
    @Environment(AppStore.self) private var store

    @State private var showWeightSheet: Bool = false
    @State private var weightSheetDate: Date = Date()
    @State private var journalMonth: Date = Date()
    @State private var trendRange: TrendRange = .week
    @State private var photoItem: PhotosPickerItem?
    @State private var showBMIEditor: Bool = false

    private var profile: UserProfile { store.profile }

    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                Text("Progress")
                    .font(.system(size: 32, weight: .bold))
                    .foregroundStyle(Theme.ink)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 20)
                    .padding(.top, 8)

                HStack(alignment: .top, spacing: 12) {
                    weightCard
                    bmiCard
                }
                .fixedSize(horizontal: false, vertical: true)
                .padding(.horizontal, 20)

                HStack(alignment: .top, spacing: 12) {
                    StatTile(
                        icon: "flame.fill",
                        iconColor: Theme.flame,
                        title: "Day Streak",
                        value: "\(store.streak)",
                        unit: "days"
                    ) { EmptyView() }

                    StatTile(
                        icon: "bolt.fill",
                        iconColor: Theme.fat,
                        title: "Avg Calories",
                        value: "\(store.averageCalories().average)",
                        unit: ""
                    ) {
                        Text("\(store.averageCalories().logged)/7 days logged")
                            .font(.system(size: 12))
                            .foregroundStyle(Theme.inkFaint)
                    }
                }
                .fixedSize(horizontal: false, vertical: true)
                .padding(.horizontal, 20)

                journalCard
                    .padding(.horizontal, 20)

                trendCard
                    .padding(.horizontal, 20)

                photosSection
            }
            .padding(.bottom, 148)
        }
        .scrollIndicators(.hidden)
        .sheet(isPresented: $showWeightSheet) {
            WeightEntrySheet(date: weightSheetDate)
                .presentationDetents([.height(340)])
        }
        .sheet(isPresented: $showBMIEditor) {
            BodyMetricsSheet()
                .presentationDetents([.height(380)])
        }
        .onChange(of: photoItem) { _, item in
            guard let item else { return }
            Task {
                if let data = try? await item.loadTransferable(type: Data.self),
                   let image = UIImage(data: data) {
                    await MainActor.run {
                        store.addProgressPhoto(image)
                        Haptics.success()
                        photoItem = nil
                    }
                }
            }
        }
    }

    // MARK: - Weight

    private var weightDelta: Double {
        profile.currentWeightKg - profile.startWeightKg
    }

    private var weightCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Weight")
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(Theme.inkSoft)

            HStack(alignment: .firstTextBaseline, spacing: 4) {
                Text(String(format: "%.1f", profile.currentWeightKg))
                    .font(.metric(32, .bold))
                    .foregroundStyle(Theme.ink)
                    .contentTransition(.numericText(value: profile.currentWeightKg))
                Text("kg").font(.system(size: 14, weight: .medium)).foregroundStyle(Theme.inkFaint)
            }

            HStack(spacing: 4) {
                Image(systemName: weightDelta >= 0 ? "arrow.up.right" : "arrow.down.right")
                    .font(.system(size: 11, weight: .bold))
                Text(String(format: "%+.1f", weightDelta))
                    .font(.system(size: 13, weight: .semibold))
            }
            .foregroundStyle(deltaColor)

            // Anchors the button to the bottom so the shorter card fills the
            // height it shares with the BMI card instead of trailing empty space.
            Spacer(minLength: 2)

            Button {
                Haptics.tap()
                weightSheetDate = Date()
                showWeightSheet = true
            } label: {
                Image(systemName: "plus")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(.white)
                    .frame(width: 38, height: 38)
                    .background(Theme.ink, in: Circle())
            }
            .buttonStyle(PressableButtonStyle())
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .cardStyle(radius: 22, padding: 16)
    }

    private var deltaColor: Color {
        switch profile.goal {
        case .lose: weightDelta <= 0 ? Theme.mint : Theme.flame
        case .gain: weightDelta >= 0 ? Theme.mint : Theme.flame
        case .maintain: Theme.inkSoft
        }
    }

    private var bmiCard: some View {
        let bmi = profile.bmi
        let category = BMICategory.from(bmi)
        let position = min(max((bmi - 15) / 25, 0), 1)

        return VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("BMI")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(Theme.inkSoft)
                Spacer()
                Button {
                    Haptics.tap()
                    showBMIEditor = true
                } label: {
                    Image(systemName: "pencil")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(Theme.inkFaint)
                }
            }

            Text(String(format: "%.1f", bmi))
                .font(.metric(32, .bold))
                .foregroundStyle(Theme.ink)

            Text(category.rawValue)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(bmiColor(category))

            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule()
                        .fill(
                            LinearGradient(
                                colors: [Theme.carbs, Theme.mint, Theme.fat, Theme.protein],
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        )
                        .frame(height: 6)
                    Circle()
                        .fill(.white)
                        .frame(width: 13, height: 13)
                        .shadow(color: .black.opacity(0.2), radius: 3, y: 1)
                        .offset(x: geo.size.width * position - 6.5)
                }
                .frame(height: 13)
            }
            .frame(height: 13)

            Text("Source:\nWHO BMI Classification")
                .font(.system(size: 10))
                .foregroundStyle(Theme.inkFaint)
                .multilineTextAlignment(.leading)
                .lineSpacing(1)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .cardStyle(radius: 22, padding: 16)
    }

    private func bmiColor(_ category: BMICategory) -> Color {
        switch category {
        case .under: Theme.carbs
        case .normal: Theme.mint
        case .over: Theme.fat
        case .obese: Theme.protein
        }
    }

    // MARK: - Weight journal

    private var journalCard: some View {
        let calendar = Calendar.current
        let monthStart = calendar.date(from: calendar.dateComponents([.year, .month], from: journalMonth)) ?? journalMonth
        let range = calendar.range(of: .day, in: .month, for: monthStart) ?? 1..<29
        let leadingBlanks = (calendar.component(.weekday, from: monthStart) - calendar.firstWeekday + 7) % 7
        let loggedInMonth = store.weightEntries.filter {
            calendar.isDate($0.date, equalTo: monthStart, toGranularity: .month)
        }.count

        return VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 3) {
                    Text("Weight Journal")
                        .font(.system(size: 19, weight: .bold))
                        .foregroundStyle(Theme.ink)
                    Text("\(loggedInMonth)/\(range.count) days logged")
                        .font(.system(size: 13))
                        .foregroundStyle(Theme.inkFaint)
                }
                Spacer()
                HStack(spacing: 10) {
                    monthButton("chevron.left") { shiftMonth(-1) }
                    Text(monthStart.formatted(.dateTime.month(.abbreviated).year()))
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Theme.ink)
                        .frame(minWidth: 74)
                    monthButton("chevron.right") { shiftMonth(1) }
                }
            }

            HStack(spacing: 0) {
                ForEach(weekdaySymbols, id: \.self) { symbol in
                    Text(symbol)
                        .font(.system(size: 11, weight: .medium))
                        .foregroundStyle(Theme.inkFaint)
                        .frame(maxWidth: .infinity)
                }
            }

            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 4), count: 7), spacing: 6) {
                ForEach(0..<leadingBlanks, id: \.self) { index in
                    Color.clear.frame(height: 42).id("blank\(index)")
                }
                ForEach(range, id: \.self) { day in
                    if let date = calendar.date(byAdding: .day, value: day - 1, to: monthStart) {
                        journalCell(date)
                    }
                }
            }
        }
        .cardStyle(radius: 26, padding: 18)
    }

    private var weekdaySymbols: [String] {
        let symbols = Calendar.current.veryShortStandaloneWeekdaySymbols
        let first = Calendar.current.firstWeekday - 1
        return Array(symbols[first...] + symbols[..<first])
    }

    private func journalCell(_ date: Date) -> some View {
        let calendar = Calendar.current
        let entry = store.weight(on: date)
        let isFuture = date > Date()
        let isToday = calendar.isDateInToday(date)

        return Button {
            guard !isFuture else { return }
            Haptics.tap()
            weightSheetDate = date
            showWeightSheet = true
        } label: {
            VStack(spacing: 1) {
                Text("\(calendar.component(.day, from: date))")
                    .font(.system(size: 12, weight: entry != nil ? .bold : .regular))
                    .foregroundStyle(entry != nil ? .white : (isFuture ? Theme.inkFaint.opacity(0.4) : Theme.inkSoft))
                if let entry {
                    Text(String(format: "%.0f", entry.kilograms))
                        .font(.system(size: 9, weight: .semibold))
                        .foregroundStyle(.white.opacity(0.85))
                } else if !isFuture {
                    Image(systemName: "plus")
                        .font(.system(size: 8, weight: .bold))
                        .foregroundStyle(Theme.inkFaint.opacity(0.6))
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 42)
            .background {
                RoundedRectangle(cornerRadius: 11, style: .continuous)
                    .fill(entry != nil ? Theme.ink : Color.clear)
                    .overlay {
                        RoundedRectangle(cornerRadius: 11, style: .continuous)
                            .strokeBorder(isToday ? Theme.ink : Color.clear, lineWidth: 1.5)
                    }
            }
        }
        .buttonStyle(PressableButtonStyle())
        .disabled(isFuture)
    }

    private func monthButton(_ symbol: String, action: @escaping () -> Void) -> some View {
        Button {
            Haptics.selection()
            withAnimation(.spring(response: 0.3, dampingFraction: 0.85)) { action() }
        } label: {
            Image(systemName: symbol)
                .font(.system(size: 12, weight: .bold))
                .foregroundStyle(Theme.ink)
                .frame(width: 30, height: 30)
                .background(Theme.well, in: Circle())
        }
        .buttonStyle(PressableButtonStyle())
    }

    private func shiftMonth(_ delta: Int) {
        if let next = Calendar.current.date(byAdding: .month, value: delta, to: journalMonth) {
            journalMonth = next
        }
    }

    // MARK: - Trend

    enum TrendRange: String, CaseIterable, Identifiable {
        case week = "Week"
        case month = "Month"
        var id: String { rawValue }
        var days: Int { self == .week ? 7 : 30 }
    }

    private var trendEntries: [WeightEntry] {
        let cutoff = Calendar.current.date(byAdding: .day, value: -trendRange.days, to: Date()) ?? Date()
        let filtered = store.weightEntries.filter { $0.date >= cutoff }
        return filtered.isEmpty ? Array(store.weightEntries.suffix(2)) : filtered
    }

    private var trendCard: some View {
        let entries = trendEntries
        let values = entries.map(\.kilograms)
        let change = (values.last ?? 0) - (values.first ?? 0)

        return VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 3) {
                    Text("Weight Trend")
                        .font(.system(size: 19, weight: .bold))
                        .foregroundStyle(Theme.ink)
                    if entries.count > 1 {
                        HStack(spacing: 4) {
                            Image(systemName: change >= 0 ? "arrow.up.right" : "arrow.down.right")
                                .font(.system(size: 10, weight: .bold))
                            Text(String(format: "%+.1f kg", change))
                                .font(.system(size: 13, weight: .bold))
                            Text("this \(trendRange.rawValue.lowercased())")
                                .font(.system(size: 13))
                                .foregroundStyle(Theme.inkFaint)
                        }
                        .foregroundStyle(change >= 0 ? Theme.flame : Theme.mint)
                    }
                }
                Spacer()
                Picker("Range", selection: $trendRange) {
                    ForEach(TrendRange.allCases) { Text($0.rawValue).tag($0) }
                }
                .pickerStyle(.segmented)
                .frame(width: 150)
            }

            if entries.count > 1 {
                HStack(spacing: 0) {
                    trendStat("Entries", "\(entries.count)", "list.bullet")
                    Divider().frame(height: 30).overlay(Theme.hairline)
                    trendStat("Lowest", String(format: "%.1f", values.min() ?? 0), "arrow.down")
                    Divider().frame(height: 30).overlay(Theme.hairline)
                    trendStat("Highest", String(format: "%.1f", values.max() ?? 0), "arrow.up")
                    Divider().frame(height: 30).overlay(Theme.hairline)
                    trendStat("Average", String(format: "%.1f", values.reduce(0, +) / Double(values.count)), "equal")
                }
                .padding(.vertical, 10)
                .background(Theme.well.opacity(0.7), in: RoundedRectangle(cornerRadius: 16, style: .continuous))

                Chart {
                    ForEach(entries) { entry in
                        AreaMark(
                            x: .value("Date", entry.date),
                            y: .value("Weight", entry.kilograms)
                        )
                        .interpolationMethod(.catmullRom)
                        .foregroundStyle(
                            LinearGradient(
                                colors: [Theme.ink.opacity(0.16), Theme.ink.opacity(0.01)],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                        )

                        LineMark(
                            x: .value("Date", entry.date),
                            y: .value("Weight", entry.kilograms)
                        )
                        .interpolationMethod(.catmullRom)
                        .lineStyle(StrokeStyle(lineWidth: 2.5, lineCap: .round))
                        .foregroundStyle(Theme.ink)

                        PointMark(
                            x: .value("Date", entry.date),
                            y: .value("Weight", entry.kilograms)
                        )
                        .symbolSize(34)
                        .foregroundStyle(Theme.ink)
                    }

                    RuleMark(y: .value("Goal", profile.goalWeightKg))
                        .lineStyle(StrokeStyle(lineWidth: 1, dash: [4, 4]))
                        .foregroundStyle(Theme.mint.opacity(0.7))
                        .annotation(position: .top, alignment: .trailing) {
                            Text("Goal")
                                .font(.system(size: 10, weight: .semibold))
                                .foregroundStyle(Theme.mint)
                        }
                }
                .chartYAxis {
                    AxisMarks(position: .leading) { value in
                        AxisGridLine().foregroundStyle(Theme.hairline)
                        AxisValueLabel {
                            if let raw = value.as(Double.self) {
                                Text(String(format: "%.0f", raw))
                                    .font(.system(size: 10))
                                    .foregroundStyle(Theme.inkFaint)
                            }
                        }
                    }
                }
                .chartXAxis {
                    AxisMarks(values: .automatic(desiredCount: 4)) { value in
                        AxisValueLabel {
                            if let date = value.as(Date.self) {
                                Text(date.formatted(.dateTime.month(.abbreviated).day()))
                                    .font(.system(size: 10))
                                    .foregroundStyle(Theme.inkFaint)
                            }
                        }
                    }
                }
                .frame(height: 190)
            } else {
                VStack(spacing: 8) {
                    Image(systemName: "chart.xyaxis.line")
                        .font(.system(size: 26))
                        .foregroundStyle(Theme.inkFaint)
                    Text("Log your weight twice to see a trend")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundStyle(Theme.inkSoft)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 40)
            }
        }
        .cardStyle(radius: 26, padding: 18)
    }

    private func trendStat(_ title: String, _ value: String, _ icon: String) -> some View {
        VStack(spacing: 4) {
            HStack(spacing: 3) {
                Image(systemName: icon).font(.system(size: 9, weight: .semibold))
                Text(title).font(.system(size: 11, weight: .medium))
            }
            .foregroundStyle(Theme.inkFaint)
            Text(value).font(.metric(16, .bold)).foregroundStyle(Theme.ink)
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Photos

    private var photosSection: some View {
        VStack(spacing: 12) {
            HStack {
                Text("Progress Photos")
                    .font(.system(size: 19, weight: .bold))
                    .foregroundStyle(Theme.ink)
                Spacer()
                PhotosPicker(selection: $photoItem, matching: .images) {
                    Label("Add", systemImage: "plus")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Theme.ink)
                }
            }
            .padding(.horizontal, 20)

            if store.photos.isEmpty {
                VStack(spacing: 8) {
                    Image(systemName: "camera")
                        .font(.system(size: 28))
                        .foregroundStyle(Theme.inkFaint)
                    Text("Track your transformation")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(Theme.inkSoft)
                    Text("Add photos to see your progress over time")
                        .font(.system(size: 13))
                        .foregroundStyle(Theme.inkFaint)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 40)
                .cardStyle(radius: 26, padding: 0)
                .padding(.horizontal, 20)
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        ForEach(store.photos) { photo in
                            if let image = store.image(named: photo.fileName) {
                                Color(.secondarySystemBackground)
                                    .frame(width: 140, height: 190)
                                    .overlay {
                                        Image(uiImage: image)
                                            .resizable()
                                            .aspectRatio(contentMode: .fill)
                                            .allowsHitTesting(false)
                                    }
                                    .clipShape(.rect(cornerRadius: 20, style: .continuous))
                                    .overlay(alignment: .bottom) {
                                        VStack(spacing: 1) {
                                            Text(photo.date.formatted(.dateTime.month(.abbreviated).day()))
                                                .font(.system(size: 12, weight: .bold))
                                            if let kg = photo.weightKg {
                                                Text(String(format: "%.1f kg", kg))
                                                    .font(.system(size: 10, weight: .medium))
                                                    .opacity(0.85)
                                            }
                                        }
                                        .foregroundStyle(.white)
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 6)
                                        .background(.black.opacity(0.4), in: Capsule())
                                        .padding(.bottom, 10)
                                    }
                                    .contextMenu {
                                        Button(role: .destructive) {
                                            store.deletePhoto(photo)
                                        } label: {
                                            Label("Delete", systemImage: "trash")
                                        }
                                    }
                            }
                        }
                    }
                }
                .contentMargins(.horizontal, 20, for: .scrollContent)
            }
        }
        .padding(.top, 6)
    }
}

// MARK: - Weight entry sheet

struct WeightEntrySheet: View {
    @Environment(AppStore.self) private var store
    @Environment(\.dismiss) private var dismiss

    let date: Date
    @State private var value: Double = 75

    var body: some View {
        VStack(spacing: 22) {
            Capsule().fill(Theme.inkFaint.opacity(0.35)).frame(width: 38, height: 5).padding(.top, 10)

            VStack(spacing: 4) {
                Text(Calendar.current.isDateInToday(date) ? "Today's weight" : date.formatted(date: .abbreviated, time: .omitted))
                    .font(.system(size: 19, weight: .bold))
                    .foregroundStyle(Theme.ink)
                Text("Small daily swings are normal — trends matter.")
                    .font(.system(size: 13))
                    .foregroundStyle(Theme.inkFaint)
            }

            HStack(alignment: .firstTextBaseline, spacing: 4) {
                Text(String(format: "%.1f", value))
                    .font(.metric(52, .bold))
                    .foregroundStyle(Theme.ink)
                    .contentTransition(.numericText(value: value))
                Text("kg").font(.system(size: 18, weight: .medium)).foregroundStyle(Theme.inkFaint)
            }

            Slider(value: $value, in: 35...200, step: 0.1)
                .tint(Theme.ink)
                .padding(.horizontal, 26)
                .onChange(of: value) { _, _ in Haptics.selection() }

            Button {
                store.logWeight((value * 10).rounded() / 10, on: date)
                Haptics.success()
                dismiss()
            } label: {
                Text("Save weight")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(Theme.ink, in: Capsule())
            }
            .buttonStyle(PressableButtonStyle())
            .padding(.horizontal, 22)

            Spacer(minLength: 0)
        }
        .background(Theme.backdrop)
        .onAppear {
            value = store.weight(on: date)?.kilograms ?? store.profile.currentWeightKg
        }
    }
}

// MARK: - Body metrics sheet

struct BodyMetricsSheet: View {
    @Environment(AppStore.self) private var store
    @Environment(\.dismiss) private var dismiss

    @State private var height: Double = 175
    @State private var weight: Double = 75

    var body: some View {
        VStack(spacing: 20) {
            Capsule().fill(Theme.inkFaint.opacity(0.35)).frame(width: 38, height: 5).padding(.top, 10)

            Text("Body metrics")
                .font(.system(size: 19, weight: .bold))
                .foregroundStyle(Theme.ink)

            VStack(spacing: 16) {
                metricRow(title: "Height", value: String(format: "%.0f cm", height)) {
                    Slider(value: $height, in: 130...220, step: 1).tint(Theme.ink)
                }
                metricRow(title: "Weight", value: String(format: "%.1f kg", weight)) {
                    Slider(value: $weight, in: 35...200, step: 0.1).tint(Theme.ink)
                }
            }
            .padding(.horizontal, 22)

            HStack(spacing: 6) {
                Text("BMI")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(Theme.inkSoft)
                Text(String(format: "%.1f", bmi))
                    .font(.metric(17, .bold))
                    .foregroundStyle(Theme.ink)
                Text(BMICategory.from(bmi).rawValue)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(Theme.inkSoft)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 9)
            .background(Theme.well, in: Capsule())

            Button {
                var profile = store.profile
                profile.heightCm = height
                profile.currentWeightKg = weight
                store.profile = profile
                store.logWeight((weight * 10).rounded() / 10)
                Haptics.success()
                dismiss()
            } label: {
                Text("Save")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(Theme.ink, in: Capsule())
            }
            .buttonStyle(PressableButtonStyle())
            .padding(.horizontal, 22)

            Spacer(minLength: 0)
        }
        .background(Theme.backdrop)
        .onAppear {
            height = store.profile.heightCm
            weight = store.profile.currentWeightKg
        }
    }

    private var bmi: Double {
        let m = height / 100
        return m > 0 ? weight / (m * m) : 0
    }

    private func metricRow<Control: View>(
        title: String,
        value: String,
        @ViewBuilder control: () -> Control
    ) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(title).font(.system(size: 14, weight: .semibold)).foregroundStyle(Theme.ink)
                Spacer()
                Text(value).font(.metric(15, .bold)).foregroundStyle(Theme.inkSoft)
            }
            control()
        }
    }
}
