package Classes.NetworkCommunication;

public interface Setable<T> {
    void handle(T newItem, Mode mode, int iIndex, int jIndex);
}
