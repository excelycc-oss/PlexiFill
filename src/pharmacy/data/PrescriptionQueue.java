package pharmacy.data;

import pharmacy.model.Prescription;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

// Priority fill queue — high priority first, FIFO within each group.
public class PrescriptionQueue {
    private final Deque<Prescription> highPriority = new ArrayDeque<>();
    private final Deque<Prescription> regular = new ArrayDeque<>();

    public void enqueue(Prescription rx) {
        if (rx.isHighPriority()) {
            highPriority.addLast(rx);
        } else {
            regular.addLast(rx);
        }
    }

    /** Returns all queued prescriptions: high priority first, then regular. */
    public List<Prescription> getAll() {
        List<Prescription> all = new ArrayList<>();
        all.addAll(highPriority);
        all.addAll(regular);
        return all;
    }

    /** Removes and returns the prescription at the given 0-based index from getAll(). */
    public Prescription removeAt(int index) {
        List<Prescription> all = getAll();
        if (index < 0 || index >= all.size()) return null;
        Prescription rx = all.get(index);
        highPriority.remove(rx);
        regular.remove(rx);
        return rx;
    }

    public boolean isEmpty() {
        return highPriority.isEmpty() && regular.isEmpty();
    }

    public int size() {
        return highPriority.size() + regular.size();
    }
}
