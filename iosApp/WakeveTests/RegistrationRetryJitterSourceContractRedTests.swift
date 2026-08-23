import XCTest
@testable import Wakeve

final class RegistrationRetryJitterSourceContractRedTests: XCTestCase {
    func testPureFullJitterDelaySaturatesAtSixtySecondsBeyondAdapterRetryBudget() {
        // This policy stays independently testable above the adapter's reviewed
        // maxAttempts = 3; no caller is authorized to increase that budget.
        XCTAssertEqual(RegistrationRetryBackoff.fullJitterDelay(attempt: 1, unitInterval: 0), 0)
        XCTAssertEqual(RegistrationRetryBackoff.fullJitterDelay(attempt: 1, unitInterval: 1), 1)
        XCTAssertEqual(RegistrationRetryBackoff.fullJitterDelay(attempt: 2, unitInterval: 1), 2)
        XCTAssertEqual(RegistrationRetryBackoff.fullJitterDelay(attempt: 3, unitInterval: 0.25), 1)
        XCTAssertEqual(RegistrationRetryBackoff.fullJitterDelay(attempt: 7, unitInterval: 1), 60)
        XCTAssertEqual(RegistrationRetryBackoff.fullJitterDelay(attempt: 100, unitInterval: 1), 60)
        XCTAssertEqual(RegistrationRetryBackoff.fullJitterDelay(attempt: 100, unitInterval: 0.5), 30)
    }
}
