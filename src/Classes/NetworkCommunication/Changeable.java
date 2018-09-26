package Classes.NetworkCommunication;

public interface Changeable<T> {
    void handle(T newItem, Mode mode, int iIndex, int jIndex);
}
