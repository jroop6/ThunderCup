package Classes.NetworkCommunication;

import java.io.Serializable;
import java.util.Arrays;

import static Classes.NetworkCommunication.Player.HOST_ID;

public class SynchronizedArray<T extends Comparable<T> & Serializable> extends SynchronizedData<T[][]> {
    public SynchronizedArray(String name, T[][] data, Precedence precedence, long parentID, Synchronizer synchronizer){
        super(name, parentID, synchronizer, precedence, 24);
        setTo(data);
    }

    public SynchronizedArray(SynchronizedArray<T> other, Synchronizer synchronizer){
        super(other.getName(), other.getParentID(), synchronizer, other.getPrecedence(), other.getSynchTolerance());
        // Create a new T[][] array with deep copies of each array element:
        T[][] arrayCopy = Arrays.copyOf(other.data,other.data.length); // creates an array of the right height
        for(int i=0; i<other.data.length; i++){
            T[] row = other.data[i];
            T[] rowCopy = Arrays.copyOf(row,row.length); // creates a row of the right width
            for(int j=0; j<row.length; j++){
                rowCopy[j] = deepCopyDataElement(row[j]); // deeply copies an element
            }
            arrayCopy[i] = rowCopy;
        }
        setTo(arrayCopy);
    }

    public SynchronizedArray<T> copyForNetworking(Synchronizer synchronizer){
        return new SynchronizedArray<>(this, synchronizer);
    }

    public int compareTo(SynchronizedData<T[][]> other){
        if(Arrays.deepEquals(data,other.data)) return 0;
        else return -1;
    }

    public void setModify(T newItem, int iPos, int jPos){
        synchronized (synchronizer){
            data[iPos][jPos] = newItem;
            if(getExternalSetter()!=null) getExternalSetter().handle(data, Mode.SET, iPos, jPos);
        }
    }

    public void changeModify(T newItem, int iPos, int jPos){
        synchronized (synchronizer){
            data[iPos][jPos] = newItem;
            if(getExternalChanger()!=null) getExternalChanger().handle(data, Mode.SET, iPos, jPos);
            getSynchronizer().addToChangedData(this);
        }
    }

    @Override
    // note: newList must have the same dimensions as data.
    public void setTo(T[][] newArray){
        synchronized (synchronizer){
            if(data==null) data = newArray; // To avoid a NullPointerException during instantiation of this class.
            for(int i=0; i<newArray.length; i++){
                T[] row = newArray[i];
                for(int j=0; j<row.length; j++){
                    setModify(newArray[i][j],i,j);
                }
            }
        }
    }

    @Override
    // note: newList must have the same dimensions as data.
    public void changeTo(T[][] newArray){
        synchronized(synchronizer){
            if(data==null) data = newArray; // To avoid a NullPointerException.
            for(int i=0; i<newArray.length; i++){
                T[] row = newArray[i];
                for(int j=0; j<row.length; j++){
                    changeModify(newArray[i][j],i,j);
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

        myArray.setModify(3,1,1);
        synchronizer.printData();
        System.out.println("number of changed datas: " + synchronizer.getChangedData().size());

        myArray.changeModify(5,2,1);
        myArray.changeModify(5,1,1);
        synchronizer.printData();
        System.out.println("number of changed datas: " + synchronizer.getChangedData().size());

        Integer[] array4 = new Integer[]{1,1};
        Integer[] array5 = new Integer[]{1,5};
        Integer[] array6 = new Integer[]{1,5};
        Integer[][] arrayB = new Integer[][] {array4, array5, array6};
        SynchronizedArray<Integer> myArray2 = new SynchronizedArray<>("test2", arrayB, Precedence.CLIENT, 21, synchronizer);

        synchronizer.printData();

        System.out.println("comparing the two arrays: " + myArray.compareTo(myArray2));
        myArray2.setModify(4,0,1);
        System.out.println("comparing again: " + myArray.compareTo(myArray2));



    }
}
