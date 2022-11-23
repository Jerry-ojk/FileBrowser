package jerry.filebrowser.file;

import java.util.ArrayList;

public class Select {

    private ArrayList<BaseFile> fileList;
    private boolean[] selectArray;
    private int selectCount = 0;


    public void onIntoMultipleSelectMode(ArrayList<BaseFile> fileList) {
        this.fileList = fileList;
        final int size = fileList.size();
        selectArray = new boolean[size];
        for (int i = 0; i < size; i++) {
            selectArray[i] = false;
        }
        selectCount = 0;

    }

    public void add(int position) {
        selectArray[position] = true;
        selectCount++;
    }

    public void remove(int position) {
        selectArray[position] = false;
        selectCount--;
    }

    public boolean isSelect(int position) {
        if (selectArray == null) return false;
        return selectArray[position];
    }


    public void selectAll() {
        final int size = selectArray.length;
        for (int i = 0; i < size; i++) {
            selectArray[i] = true;
        }
        selectCount = size;
    }

    public void selectSingle(int position) {
        final int size = selectArray.length;
        for (int i = 0; i < size; i++) {
            selectArray[i] = false;
        }
        selectArray[position] = true;
        selectCount = 1;
    }

    public void clearSelect() {
        if (selectCount == 0) return;
        final int size = selectArray.length;
        for (int i = 0; i < size; i++) {
            selectArray[i] = false;
        }
        selectCount = 0;
    }


    public boolean isSelectedAll() {
        return selectCount == selectArray.length;
    }

    public void selectReverse() {
        final int size = selectArray.length;
        for (int i = 0; i < size; i++) {
            selectArray[i] = !selectArray[i];
        }
        selectCount = size - selectCount;
    }


    public ArrayList<BaseFile> getSelectList() {
        final int size = selectArray.length;
        if (fileList.size() != size) {
            return null;
        }
        ArrayList<BaseFile> result = new ArrayList<>(selectCount);
        for (int i = 0; i < size; i++) {
            if (selectArray[i]) {
                result.add(fileList.get(i));
            }
        }
        return result;
    }

    public int getSelectCount() {
        return selectCount;
    }

    public void clear() {
        if (fileList != null) {
            fileList = null;
        }
        if (selectCount == 0) return;
        final int size = selectArray.length;
        for (int i = 0; i < size; i++) {
            selectArray[i] = false;
        }
        selectCount = 0;

    }


    public void onQuitMultipleSelectMode() {
        clear();
    }
}
