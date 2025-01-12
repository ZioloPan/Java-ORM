package orm;

public class CustomList<T> {
    public ElementList<T> head;

    public CustomList() {
        this.head = null;
    }

    public void add(T element) {
        ElementList<T> newElement = new ElementList<>();
        newElement.current = element;
        newElement.next = null;

        if (head == null) {
            head = newElement;
        } else {
            ElementList<T> lastElement = head;
            while (lastElement.next != null) {
                lastElement = lastElement.next;
            }
            lastElement.next = newElement;
        }
    }

    public int size() {
        int result = 0;

        ElementList<T> currentElement = head;
        while (currentElement != null) {
            result++;
            currentElement = currentElement.next;
        }
        return result;
    }

    public T get(int index) {
        if (index < 0 || index >= size()) throw new IndexOutOfBoundsException("Index out of bound");

        ElementList<T> currentElement = head;
        for (int i = 0; i < index; i++) {
            currentElement = currentElement.next;
        }
        return currentElement.current;
    }

    public void remove(int index) {
        if (index < 0 || index >= size()) throw new IndexOutOfBoundsException("Index out of bound");

        if (index == 0) {
            head = head.next;
        } else {
            ElementList<T> currentElement = head;
            for (int i = 0; i < index - 1; i++) {
                currentElement = currentElement.next;
            }
            currentElement.next = currentElement.next.next;
        }
    }

    public int findIndex(T element) {
        int index = 0;

        ElementList<T> currentElement = head;
        while (currentElement != null) {
            if (element.equals(currentElement.current)) {
                return index;
            }

            index++;
            currentElement = currentElement.next;
        }

        return -1;
    }
}
