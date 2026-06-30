package pharmacy.model;

import java.util.List;

public class Medication {
    private final String name;
    private final List<String> strengths;

    public Medication(String name, List<String> strengths) {
        this.name = name;
        this.strengths = strengths;
    }

    public String getName() {
        return name;
    }

    public List<String> getStrengths() {
        return strengths;
    }
}
