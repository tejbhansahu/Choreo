import SwiftUI
import Choreo // the Kotlin Multiplatform framework produced by the :choreo module

// Mirrors the Android demo: reveal many dots, paced across CADisplayLink frames so the UI stays smooth.
struct ContentView: View {
    private let total = 15_000
    private let pacer = ChoreoKt.defaultFramePacer()
    private let caps = IosDeviceProfiler().capabilities()

    @State private var dots: [Dot] = []
    @State private var renderedCount = 0
    @State private var status = "Idle — tap to render."

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Choreo").font(.largeTitle).bold()
            Text("Adaptive frame-paced rendering").foregroundStyle(.secondary)

            VStack(alignment: .leading, spacing: 4) {
                stat("Display", "\(Int(caps.refreshRateHz)) Hz · budget \(String(format: "%.1f", caps.frameBudgetMs)) ms")
                stat("CPU", "\(caps.cpuCores) cores")
                stat("Tier", "\(caps.tier.name) (score \(caps.performanceScore)/100)")
                stat("Start batch", "\(caps.recommendedChunkSize) dots/frame")
            }
            .padding(12)
            .background(.quaternary, in: RoundedRectangle(cornerRadius: 12))

            Button("Render paced") { renderPaced() }
                .buttonStyle(.borderedProminent)

            ProgressView(value: Double(renderedCount), total: Double(total))
            Text(status).font(.footnote).foregroundStyle(.secondary)

            Canvas { context, size in
                for i in 0..<min(renderedCount, dots.count) {
                    let d = dots[i]
                    let rect = CGRect(x: CGFloat(d.x) * size.width, y: CGFloat(d.y) * size.height, width: 4, height: 4)
                    context.fill(Path(ellipseIn: rect), with: .color(d.color))
                }
            }
            .frame(maxWidth: .infinity, minHeight: 320)
            .background(.quaternary, in: RoundedRectangle(cornerRadius: 12))

            Spacer()
        }
        .padding()
        .onAppear { dots = (0..<total).map { _ in Dot.random() } }
        .onDisappear { pacer.cancel() }
    }

    private func renderPaced() {
        renderedCount = 0
        status = "Pacing \(total) dots across frames…"
        let config = PacingConfig.companion.forDevice(caps: caps)
        let items: [KotlinInt] = (0..<total).map { KotlinInt(int: Int32($0)) }

        pacer.process(
            items: items,
            config: config,
            onItem: { _ in Dot.simulateWork() },           // stand-in for real per-item cost
            onProgress: { processed, _ in renderedCount = processed.intValue },
            onError: nil,
            onComplete: { s in
                status = "Paced ✓ \(s.frames) frames · \(Int(s.effectiveFps)) fps · \(s.totalTimeMs) ms"
            }
        )
    }

    private func stat(_ label: String, _ value: String) -> some View {
        HStack {
            Text(label).foregroundStyle(.secondary)
            Spacer()
            Text(value).fontWeight(.medium)
        }
        .font(.caption)
    }
}

struct Dot {
    let x: Float
    let y: Float
    let color: Color

    static let palette: [Color] = [.blue, .green, .orange, .pink, .purple, .teal]

    static func random() -> Dot {
        Dot(x: Float.random(in: 0...1), y: Float.random(in: 0...1), color: palette.randomElement()!)
    }

    @discardableResult
    static func simulateWork() -> Double {
        var acc = 0.0
        for i in 0..<120 { acc += (Double(i) * 1.3 + 1.0).squareRoot() }
        return acc
    }
}
