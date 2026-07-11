import SwiftUI
import UIKit
import AlliumTutorKit

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    private let tutorBackground = Color(
        red: 17.0 / 255.0,
        green: 19.0 / 255.0,
        blue: 16.0 / 255.0
    )

    var body: some View {
        ZStack {
            tutorBackground.ignoresSafeArea()
            ComposeView()
        }
        .preferredColorScheme(.dark)
    }
}
