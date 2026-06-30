package pharmacy.model;

public enum PrescriptionStatus {
    ON_FILE("On File"),
    IN_QUEUE("In Queue"),
    READY("Ready"),
    PICKED_UP("Picked Up");

    private final String displayName;

    PrescriptionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
