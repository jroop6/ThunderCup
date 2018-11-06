package Classes.NetworkCommunication;

import java.io.*;
import java.util.Arrays;

import static Classes.Player.HOST_ID;

public class SynchronizedArray<T extends Comparable<T> & Serializable> extends SynchronizedData<T[][]> {
    public SynchronizedArray(String name, T[][] data, Precedence precedence, long parentID, Synchronizer synchronizer){
        super(name, parentID, synchronizer, precedence, 24);
        setTo(data);
    }

    public int compareTo(SynchronizedData<T[][]> other){
        if(Arrays.deepEquals(data,other.data)) return 0;
        else return -1;
    }

    public void setModify(int iPos, int jPos, T newItem){
        synchronized (synchronizer){
            data[iPos][jPos] = newItem;
            if(getExternalSetter()!=null) getExternalSetter().handle(data, Mode.SET, iPos, jPos);
        }
    }

    public void changeModify(int iPos, int jPos, T newItem){
        synchronized (synchronizer){
            data[iPos][jPos] = newItem;
            if(getExternalChanger()!=null) getExternalChanger().handle(data, Mode.SET, iPos, jPos);
            getSynchronizer().addToChangedData(this);
        }
    }

    @Override
    // note: newArray must have the same dimensions as data.
    public void setTo(T[][] newArray){
        synchronized (synchronizer){
            if(data==null) data = newArray; // To avoid a NullPointerException during instantiation of this class.
            for(int i=0; i<newArray.length; i++){
                T[] row = newArray[i];
                for(int j=0; j<row.length; j++){
                    setModify(i, j, newArray[i][j]);
                }
            }
        }
    }

    @Override
    // note: newArray must have the same dimensions as data.
    public void changeTo(T[][] newArray){
        synchronized(synchronizer){
            if(data==null) data = newArray; // To avoid a NullPointerException.
            for(int i=0; i<newArray.length; i++){
                T[] row = newArray[i];
                for(int j=0; j<row.length; j++){
                    changeModify(i, j, newArray[i][j]);
                }
            }
        }
        getSynchronizer().addToChangedData(this);
    }


    public static void main(String[] args){
        Synchronizer synchronizer = new Synchronizer(HOST_ID);
        Integer[] array1 = new Integer[]{1,1};
        Integer[] array2 = new Integer[]{1,1};
        Integer[] array3 = new Integer[]{1,1};
        Integer[][] array = new Integer[][] {array1, array2, array3};
        SynchronizedArray<Integer> myArray = new SynchronizedArray<>("test", array, Precedence.CLIENT, 21, synchronizer);

        synchronizer.printData();

        myArray.setModify(1, 1, 3);
        synchronizer.printData();
        System.out.println("number of changed datas: " + synchronizer.getChangedData().size());

        myArray.changeModify(2, 1, 5);
        myArray.changeModify(1, 1, 5);
        synchronizer.printData();
        System.out.println("number of changed datas: " + synchronizer.getChangedData().size());

        Integer[] array4 = new Integer[]{1,1};
        Integer[] array5 = new Integer[]{1,5};
        Integer[] array6 = new Integer[]{1,5};
        Integer[][] arrayB = new Integer[][] {array4, array5, array6};
        SynchronizedArray<Integer> myArray2 = new SynchronizedArray<>("test2", arrayB, Precedence.CLIENT, 21, synchronizer);

        synchronizer.printData();

        System.out.println("comparing the two arrays: " + myArray.compareTo(myArray2));
        myArray2.setModify(0, 1, 4);
        System.out.println("comparing again: " + myArray.compareTo(myArray2));
    }
}
