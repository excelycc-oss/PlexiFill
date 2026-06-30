package pharmacy.data;

import pharmacy.model.Medication;
import pharmacy.model.Patient;
import pharmacy.model.Prescription;
import pharmacy.model.PrescriptionStatus;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

// CSV-backed store for patients, prescriptions, and medications.
// patientsByPhone maps phone -> list so a family can share one number.
public class CsvDataStore {
    private final List<Patient> patients;
    private final Map<String, List<Patient>> patientsByPhone; // phone -> one or more patients
    private final List<Prescription> prescriptions;
    private final List<Medication> medications;
    private final String patientsFile;
    private final String prescriptionsFile;
    private final String medicationsFile;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    public CsvDataStore(String dataDirectory) {
        patients = new ArrayList<>();
        patientsByPhone = new HashMap<>();
        prescriptions = new ArrayList<>();
        medications = new ArrayList<>();
        this.patientsFile = dataDirectory + File.separator + "patients.csv";
        this.prescriptionsFile = dataDirectory + File.separator + "prescriptions.csv";
        this.medicationsFile = dataDirectory + File.separator + "medications.csv";
        loadPatients();
        loadPrescriptions();
        loadMedications();
    }

    // Load

    private void loadPatients() {
        File file = new File(patientsFile);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            reader.readLine(); // skip header
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = parseCsvLine(line);
                if (parts.length >= 4) {
                    String firstName = parts[0].trim();
                    String lastName  = parts[1].trim();
                    LocalDate dob    = LocalDate.parse(parts[2].trim(), DATE_FORMAT);
                    String phone     = parts[3].trim();
                    Patient p = new Patient(firstName, lastName, dob, phone);
                    patients.add(p);
                    patientsByPhone.computeIfAbsent(phone, k -> new ArrayList<>()).add(p);
                }
            }
        } catch (Exception e) {
            System.out.println("  Warning: Could not read " + patientsFile + " - " + e.getMessage());
        }
    }

    private void loadPrescriptions() {
        File file = new File(prescriptionsFile);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            reader.readLine(); // skip header
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = parseCsvLine(line);
                if (parts.length >= 5) {
                    String patientPhone       = parts[0].trim();
                    String medName            = parts[1].trim();
                    String strength           = parts[2].trim();
                    int daySupply             = Integer.parseInt(parts[3].trim());
                    PrescriptionStatus status = parseStatus(parts[4].trim());
                    String priority           = parts.length >= 6 ? parts[5].trim() : "";
                    LocalDate refillDate      = null;
                    if (parts.length >= 7 && !parts[6].trim().isEmpty()) {
                        refillDate = LocalDate.parse(parts[6].trim(), DATE_FORMAT);
                    }
                    LocalDate pickupDate      = null;
                    if (parts.length >= 8 && !parts[7].trim().isEmpty()) {
                        pickupDate = LocalDate.parse(parts[7].trim(), DATE_FORMAT);
                    }
                    int refillsRemaining = 0;
                    if (parts.length >= 9 && !parts[8].trim().isEmpty()) {
                        try { refillsRemaining = Integer.parseInt(parts[8].trim()); } catch (NumberFormatException ignored) {}
                    }
                    Prescription rx = new Prescription(
                            patientPhone, medName, strength, daySupply, status, priority, refillDate);
                    if (pickupDate != null) rx.setPickupDate(pickupDate);
                    rx.setRefillsRemaining(refillsRemaining);
                    prescriptions.add(rx);
                }
            }
        } catch (Exception e) {
            System.out.println("  Warning: Could not read " + prescriptionsFile + " - " + e.getMessage());
        }
    }

    private void loadMedications() {
        File file = new File(medicationsFile);
        if (!file.exists()) {
            System.out.println("  Warning: medications.csv not found.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            reader.readLine(); // skip header
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = parseCsvLine(line);
                if (parts.length >= 2) {
                    String name = parts[0].trim();
                    List<String> strengths = Arrays.asList(parts[1].trim().split(","));
                    medications.add(new Medication(name, strengths));
                }
            }
        } catch (Exception e) {
            System.out.println("  Warning: Could not read " + medicationsFile + " - " + e.getMessage());
        }
    }

    // Save

    private void savePatients() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(patientsFile))) {
            writer.println("firstName,lastName,dateOfBirth,phoneNumber");
            for (Patient p : patients) {
                writer.println(escapeCsv(p.getFirstName()) + ","
                        + escapeCsv(p.getLastName()) + ","
                        + p.getFormattedDob() + ","
                        + escapeCsv(p.getPhoneNumber()));
            }
        } catch (IOException e) {
            System.out.println("  Error: Could not save patients - " + e.getMessage());
        }
    }

    public void savePrescriptions() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(prescriptionsFile))) {
            writer.println("patientPhone,medicationName,strength,daySupply,status,priority,refillDate,pickupDate,refillsRemaining");
            for (Prescription rx : prescriptions) {
                String refillDate  = rx.getRefillDate()  != null ? rx.getRefillDate().format(DATE_FORMAT)  : "";
                String pickupDate  = rx.getPickupDate()  != null ? rx.getPickupDate().format(DATE_FORMAT)  : "";
                writer.println(escapeCsv(rx.getPatientPhone()) + ","
                        + escapeCsv(rx.getMedicationName()) + ","
                        + escapeCsv(rx.getStrength()) + ","
                        + rx.getDaySupply() + ","
                        + rx.getStatus().getDisplayName() + ","
                        + escapeCsv(rx.getPriority()) + ","
                        + refillDate + ","
                        + pickupDate + ","
                        + rx.getRefillsRemaining());
            }
        } catch (IOException e) {
            System.out.println("  Error: Could not save prescriptions - " + e.getMessage());
        }
    }

    // Patients

    public Patient addPatient(String firstName, String lastName, LocalDate dob, String phoneNumber) {
        Patient patient = new Patient(firstName, lastName, dob, phoneNumber);
        patients.add(patient);
        patientsByPhone.computeIfAbsent(phoneNumber, k -> new ArrayList<>()).add(patient);
        savePatients();
        return patient;
    }

    public List<Patient> getPatients() {
        return patients;
    }

    /** Returns the first patient registered with this phone (for queue display). */
    public Patient getPatientByPhone(String phoneNumber) {
        List<Patient> list = patientsByPhone.get(phoneNumber);
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }

    /** Returns all patients registered with this phone number. */
    public List<Patient> getPatientsByPhone(String phoneNumber) {
        List<Patient> list = patientsByPhone.get(phoneNumber);
        return list == null ? Collections.emptyList() : list;
    }

    /** Returns true if any patient exists with this phone number. */
    public boolean phoneExists(String phoneNumber) {
        return patientsByPhone.containsKey(phoneNumber);
    }

    /**
     * Returns true if a patient with this exact first name, last name, AND phone already exists.
     * Used to block registering the same person twice under the same number.
     */
    public boolean phoneExistsForName(String phoneNumber, String firstName, String lastName) {
        List<Patient> list = patientsByPhone.get(phoneNumber);
        if (list == null) return false;
        for (Patient p : list) {
            if (p.getFirstName().equalsIgnoreCase(firstName)
                    && p.getLastName().equalsIgnoreCase(lastName)) {
                return true;
            }
        }
        return false;
    }

    public List<Patient> findDuplicates(String firstName, String lastName, LocalDate dob) {
        List<Patient> duplicates = new ArrayList<>();
        for (Patient p : patients) {
            if (p.getFirstName().equalsIgnoreCase(firstName)
                    && p.getLastName().equalsIgnoreCase(lastName)
                    && p.getDateOfBirth().equals(dob)) {
                duplicates.add(p);
            }
        }
        return duplicates;
    }

    // Prescriptions

    public void addPrescription(Prescription rx) {
        prescriptions.add(rx);
        savePrescriptions();
    }

    public void removePrescription(Prescription rx) {
        prescriptions.remove(rx);
        savePrescriptions();
    }

    public void deletePatient(Patient patient) {
        patients.remove(patient);
        List<Patient> phoneList = patientsByPhone.get(patient.getPhoneNumber());
        if (phoneList != null) {
            phoneList.remove(patient);
            if (phoneList.isEmpty()) patientsByPhone.remove(patient.getPhoneNumber());
        }
        prescriptions.removeIf(rx -> rx.getPatientPhone().equals(patient.getPhoneNumber())
                && isOnlyPatientOnPhone(patient));
        savePatients();
        savePrescriptions();
    }

    private boolean isOnlyPatientOnPhone(Patient patient) {
        List<Patient> list = patientsByPhone.get(patient.getPhoneNumber());
        return list == null || list.isEmpty();
    }

    public List<Prescription> getAllPrescriptions() {
        return prescriptions;
    }

    public List<Prescription> getPrescriptionsByPhone(String phoneNumber) {
        List<Prescription> result = new ArrayList<>();
        for (Prescription rx : prescriptions) {
            if (rx.getPatientPhone().equals(phoneNumber)) result.add(rx);
        }
        return result;
    }

    public List<Prescription> getReadyPrescriptionsByPhone(String phoneNumber) {
        List<Prescription> result = new ArrayList<>();
        for (Prescription rx : prescriptions) {
            if (rx.getPatientPhone().equals(phoneNumber)
                    && rx.getStatus() == PrescriptionStatus.READY) {
                result.add(rx);
            }
        }
        return result;
    }

    // Medications

    public List<Medication> getMedications() {
        return medications;
    }

    // CSV helpers

    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }

    private String escapeCsv(String value) {
        if (value.contains(",")) {
            return "\"" + value + "\"";
        }
        return value;
    }

    private PrescriptionStatus parseStatus(String text) {
        for (PrescriptionStatus s : PrescriptionStatus.values()) {
            if (s.getDisplayName().equalsIgnoreCase(text)) return s;
        }
        return PrescriptionStatus.ON_FILE;
    }
}
