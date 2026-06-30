package pharmacy.service;

import pharmacy.data.CsvDataStore;
import pharmacy.data.PrescriptionQueue;
import pharmacy.model.Medication;
import pharmacy.model.Patient;
import pharmacy.model.Prescription;
import pharmacy.model.PrescriptionStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PatientLookupService {
    private final CsvDataStore dataStore;
    private final PrescriptionQueue fillQueue;

    public PatientLookupService(CsvDataStore dataStore) {
        this.dataStore = dataStore;
        this.fillQueue = new PrescriptionQueue();
        for (Prescription rx : dataStore.getAllPrescriptions()) {
            if (rx.getStatus() == PrescriptionStatus.IN_QUEUE) {
                fillQueue.enqueue(rx);
            }
        }
    }

    // Search (patients with READY prescriptions only)

    public List<Patient> searchByLastName(String lastNameQuery) {
        List<Patient> matches = new ArrayList<>();
        String query = lastNameQuery.toLowerCase();
        for (Patient patient : dataStore.getPatients()) {
            if (patient.getLastName().toLowerCase().startsWith(query)
                    && hasReadyPrescriptions(patient.getPhoneNumber())) {
                matches.add(patient);
            }
        }
        return matches;
    }

    public List<Patient> searchByFirstAndLastName(String firstName, String lastName) {
        List<Patient> matches = new ArrayList<>();
        for (Patient patient : dataStore.getPatients()) {
            if (patient.getFirstName().toLowerCase().startsWith(firstName.toLowerCase())
                    && patient.getLastName().toLowerCase().startsWith(lastName.toLowerCase())
                    && hasReadyPrescriptions(patient.getPhoneNumber())) {
                matches.add(patient);
            }
        }
        return matches;
    }

    public List<Patient> searchByDateOfBirth(LocalDate dob) {
        List<Patient> matches = new ArrayList<>();
        for (Patient patient : dataStore.getPatients()) {
            if (patient.getDateOfBirth().equals(dob)
                    && hasReadyPrescriptions(patient.getPhoneNumber())) {
                matches.add(patient);
            }
        }
        return matches;
    }

    public List<Patient> searchByPhone(String phone) {
        List<Patient> candidates = dataStore.getPatientsByPhone(phone);
        if (candidates.isEmpty() || !hasReadyPrescriptions(phone)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(candidates);
    }

    // Search (all patients)

    public List<Patient> findAllByLastName(String lastNameQuery) {
        List<Patient> matches = new ArrayList<>();
        String query = lastNameQuery.toLowerCase();
        for (Patient patient : dataStore.getPatients()) {
            if (patient.getLastName().toLowerCase().startsWith(query)) {
                matches.add(patient);
            }
        }
        return matches;
    }

    public List<Patient> findAllByFirstAndLastName(String firstName, String lastName) {
        List<Patient> matches = new ArrayList<>();
        for (Patient patient : dataStore.getPatients()) {
            if (patient.getFirstName().toLowerCase().startsWith(firstName.toLowerCase())
                    && patient.getLastName().toLowerCase().startsWith(lastName.toLowerCase())) {
                matches.add(patient);
            }
        }
        return matches;
    }

    public List<Patient> findAllByDateOfBirth(LocalDate dob) {
        List<Patient> matches = new ArrayList<>();
        for (Patient patient : dataStore.getPatients()) {
            if (patient.getDateOfBirth().equals(dob)) {
                matches.add(patient);
            }
        }
        return matches;
    }

    public List<Patient> findAllByPhone(String phone) {
        return dataStore.getPatientsByPhone(phone);
    }

    // Prescriptions

    public boolean hasReadyPrescriptions(String phoneNumber) {
        return !dataStore.getReadyPrescriptionsByPhone(phoneNumber).isEmpty();
    }

    public List<Prescription> getReadyPrescriptions(String phoneNumber) {
        return dataStore.getReadyPrescriptionsByPhone(phoneNumber);
    }

    public List<Prescription> getAllPrescriptions(String phoneNumber) {
        return dataStore.getPrescriptionsByPhone(phoneNumber);
    }

    // Add prescription

    public void addPrescription(Prescription rx) {
        dataStore.addPrescription(rx);
        if (rx.getStatus() == PrescriptionStatus.IN_QUEUE) {
            fillQueue.enqueue(rx);
        }
    }

    // Fill queue

    public PrescriptionQueue getFillQueue() {
        return fillQueue;
    }

    public void fillPrescription(Prescription rx) {
        fillQueue.removeAt(fillQueue.getAll().indexOf(rx));
        rx.setStatus(PrescriptionStatus.READY);
        dataStore.savePrescriptions();
    }

    // Release to patient

    public void markPickedUp(Prescription rx, LocalDate pickupDate) {
        rx.setStatus(PrescriptionStatus.PICKED_UP);
        rx.setPickupDate(pickupDate);
        rx.setRefillDate(pickupDate.plusDays(Math.max(0, rx.getDaySupply() - 2)));
        dataStore.savePrescriptions();
    }

    // Refills due soon

    public List<Prescription> getRefillsDueSoon() {
        LocalDate cutoff = LocalDate.now().plusDays(3);
        List<Prescription> due = new ArrayList<>();
        for (Prescription rx : dataStore.getAllPrescriptions()) {
            if (rx.getStatus() == PrescriptionStatus.PICKED_UP
                    && rx.getRefillDate() != null
                    && !rx.getRefillDate().isAfter(cutoff)) {
                due.add(rx);
            }
        }
        due.sort((a, b) -> a.getRefillDate().compareTo(b.getRefillDate()));
        return due;
    }

    // Patients

    public Patient addPatient(String firstName, String lastName, LocalDate dob, String phoneNumber) {
        return dataStore.addPatient(firstName, lastName, dob, phoneNumber);
    }

    public Patient getPatientByPhone(String phoneNumber) {
        return dataStore.getPatientByPhone(phoneNumber);
    }

    public List<Patient> findDuplicates(String firstName, String lastName, LocalDate dob) {
        return dataStore.findDuplicates(firstName, lastName, dob);
    }

    /** True if any patient has this phone number (used for phone-number search). */
    public boolean phoneExists(String phoneNumber) {
        return dataStore.phoneExists(phoneNumber);
    }

    /** True if a patient with this exact name already uses this phone number. */
    public boolean phoneExistsForName(String phoneNumber, String firstName, String lastName) {
        return dataStore.phoneExistsForName(phoneNumber, firstName, lastName);
    }

    public void refillPrescription(Prescription rx) {
        rx.setStatus(PrescriptionStatus.IN_QUEUE);
        rx.setRefillsRemaining(rx.getRefillsRemaining() - 1);
        rx.setPickupDate(null);
        rx.setRefillDate(null);
        fillQueue.enqueue(rx);
        dataStore.savePrescriptions();
    }

    public void deletePatient(Patient patient) {
        // Remove any IN_QUEUE prescriptions from the fill queue first
        for (Prescription rx : dataStore.getPrescriptionsByPhone(patient.getPhoneNumber())) {
            if (rx.getStatus() == PrescriptionStatus.IN_QUEUE) {
                int idx = fillQueue.getAll().indexOf(rx);
                if (idx >= 0) fillQueue.removeAt(idx);
            }
        }
        dataStore.deletePatient(patient);
    }

    public void removePrescription(Prescription rx) {
        if (rx.getStatus() == PrescriptionStatus.IN_QUEUE) {
            int idx = fillQueue.getAll().indexOf(rx);
            if (idx >= 0) fillQueue.removeAt(idx);
        }
        dataStore.removePrescription(rx);
    }

    // Medications

    public List<Medication> getMedications() {
        return dataStore.getMedications();
    }
}
