import UIKit
import IntentsUI

/// View Controller for displaying Siri intent results in a custom UI
class IntentViewController: UIViewController, INUIHostedViewControlling {
    
    @IBOutlet weak var titleLabel: UILabel!
    @IBOutlet weak var subtitleLabel: UILabel!
    @IBOutlet weak var iconImageView: UIImageView!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
    }
    
    private func setupUI() {
        view.backgroundColor = .systemBackground
        
        titleLabel.textColor = .label
        titleLabel.font = .systemFont(ofSize: 17, weight: .semibold)
        
        subtitleLabel.textColor = .secondaryLabel
        subtitleLabel.font = .systemFont(ofSize: 14, weight: .regular)
        
        iconImageView.tintColor = .systemBlue
        iconImageView.contentMode = .scaleAspectFit
    }
    
    // MARK: - INUIHostedViewControlling
    
    func configureView(
        for parameters: Set<INParameter>,
        of interaction: INInteraction,
        interactiveBehavior: INUIInteractiveBehavior,
        context: INUIHostedViewContext,
        completion: @escaping (Bool, Set<INParameter>, CGSize) -> Void
    ) {
        
        guard let intent = interaction.intent else {
            completion(false, parameters, .zero)
            return
        }
        
        // Configure the view based on intent type
        configureForIntent(intent, parameters: parameters)
        
        // Return the desired size
        let desiredSize = CGSize(width: 320, height: 120)
        completion(true, parameters, desiredSize)
    }
    
    private func configureForIntent(_ intent: INIntent, parameters: Set<INParameter>) {
        // Configure UI based on intent type
        // This would be expanded based on specific intent handling
        
        titleLabel.text = "Wakeve"
        subtitleLabel.text = "Action traitée avec succès"
    }
    
    // MARK: - Convenience Methods
    
    func configureForCreateEvent(title: String, date: Date) {
        let dateFormatter = DateFormatter()
        dateFormatter.dateStyle = .medium
        dateFormatter.timeStyle = .short
        
        titleLabel.text = title
        subtitleLabel.text = dateFormatter.string(from: date)
        iconImageView.image = UIImage(systemName: "calendar.badge.plus")
    }
    
    func configureForAddSlot(date: Date, timeOfDay: String) {
        let dateFormatter = DateFormatter()
        dateFormatter.dateStyle = .medium
        dateFormatter.timeStyle = .none
        
        titleLabel.text = "Créneau ajouté"
        subtitleLabel.text = "\(dateFormatter.string(from: date)) - \(timeOfDay)"
        iconImageView.image = UIImage(systemName: "clock.badge.plus")
    }
    
    func configureForConfirmation(message: String) {
        titleLabel.text = "Confirmation"
        subtitleLabel.text = message
        iconImageView.image = UIImage(systemName: "checkmark.circle.fill")
    }
}

// MARK: - INUIHostedViewAnimating

extension IntentViewController: INUIHostedViewAnimating {
    func animateView(presentationStyle: INUIPresentationStyle, duration: TimeInterval, animations: @escaping () -> Void, completion: @escaping (Bool) -> Void) {
        // Custom animations if needed
        animations()
        completion(true)
    }
}
