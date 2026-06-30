package pharmacy.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Patient {
    private final String firstName;
    private final String lastName;
    private final LocalDate dateOfBirth;
    private final String phoneNumber;

    public Patient(String firstName, String lastName, LocalDate dateOfBirth, String phoneNumber) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.phoneNumber = phoneNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getFormattedDob() {
        return dateOfBirth.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    @Override
    public String toString() {
        return String.format("%-20s %-20s DOB: %s", firstName, lastName, getFormattedDob());
    }
}
