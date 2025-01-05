package com.maxlogic.tutorials.algorithms.sorting;

public class MergeSort {

  public static void main(String[] args) {
    int[] input = {6,8,3,5,9,2,4,7,1,9};
    mergeSort(input, 0, input.length-1);

    for(int cur:input){
      System.out.print(cur+",");
    }
  }

  private static void mergeSort(int[] arr, int start, int end) {
    if(start >= end) {
      return;
    }
    int mid = (end+start)/2;
    mergeSort(arr, start, mid);
    mergeSort(arr, mid+1, end);
    merge(arr, start, mid, end);
  }

  private static void merge(int[] arr, int low, int mid, int high) {
    int fIdx = low;
    int sIdx = mid+1;
    int[] newArr = new int[high-low+1];

    for(int i=0; i< newArr.length ;i++){

      if(fIdx <= mid && sIdx <= high) {
        if(arr[fIdx] < arr[sIdx]) {
          newArr[i] = arr[fIdx];
          fIdx++;
        } else {
          newArr[i] = arr[sIdx];
          sIdx++;
        }
      } else if (fIdx <= mid) {
        newArr[i] = arr[fIdx];
        fIdx++;
      } else if(sIdx <= high) {
        newArr[i] = arr[sIdx];
        sIdx++;
      }
    }

    fIdx = low;
    for(int j=0; j<newArr.length-1; j++) {
      arr[fIdx] = newArr[j];
      fIdx++;
    }
  }
}
