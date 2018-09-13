package Classes.NetworkCommunication;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;


public class SynchronizedData<T> {
    String name;
    T data;
    static int test = 1;

    enum SynchronizationType {SEND_UPDATES_ONLY, SEND_EVERY_TIME, ADDITIVE}
    enum Status{CHANGED, NO_CHANGES}

	public SynchronizedData(List synchronizedDataList){
        registerToList(synchronizedDataList);
    }
	public SynchronizedData(List synchronizedDataList, String name){
        registerToList(synchronizedDataList);
        this.name = name;
    }

    public void registerToList(List synchronizedDataList){
        synchronizedDataList.add(this);
    }

    public static void main(String[] args){
        List<SynchronizedData> myList = new LinkedList<>();
        SynchronizedData<Integer> synchronizedData = new SynchronizedData<>(myList);
        System.out.println("hello!");

    }
}
