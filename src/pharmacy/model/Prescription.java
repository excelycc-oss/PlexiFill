package pharmacy.model;

import java.time.LocalDate;

public class Prescription {
    private final String patientPhone;
    private final String medicationName;
    private final String strength;
    private final int daySupply;
    private PrescriptionStatus status;
    private final String priority; // "High", "Regular", or ""
    private LocalDate refillDate;
    private LocalDate pickupDate;
    private int refillsRemaining;

    public Prescription(String patientPhone, String medicationName,
                        String strength, int daySupply, PrescriptionStatus status,
                        String priority, LocalDate refillDate) {
        this.patientPhone = patientPhone;
        this.medicationName = medicationName;
        this.strength = strength;
        this.daySupply = daySupply;
        this.status = status;
        this.priority = priority == null ? "" : priority;
        this.refillDate = refillDate;
    }

    public String getPatientPhone()      { return patientPhone; }
    public String getMedicationName()    { return medicationName; }
    public String getStrength()          { return strength; }
    public int getDaySupply()            { return daySupply; }
    public PrescriptionStatus getStatus(){ return status; }
    public String getPriority()          { return priority; }
    public LocalDate getRefillDate()     { return refillDate; }

    public LocalDate getPickupDate()                        { return pickupDate; }
    public int  getRefillsRemaining()                       { return refillsRemaining; }
    public void setStatus(PrescriptionStatus status)        { this.status = status; }
    public void setRefillDate(LocalDate refillDate)         { this.refillDate = refillDate; }
    public void setPickupDate(LocalDate pickupDate)         { this.pickupDate = pickupDate; }
    public void setRefillsRemaining(int refillsRemaining)   { this.refillsRemaining = refillsRemaining; }

    public boolean isHighPriority() {
        return "High".equalsIgnoreCase(priority);
    }

    @Override
    public String toString() {
        String prefix = isHighPriority() ? "* " : "  ";
        String priorityTag = !priority.isEmpty() ? " [" + priority + " Priority]" : "";
        return String.format("%s%-25s %-10s %d-day [%s]%s",
                prefix, medicationName, strength, daySupply, status, priorityTag);
    }
}
