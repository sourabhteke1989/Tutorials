package com.maxlogic.tutorials.algorithms.sorting;

public class QuickSort {

  public static void main(String[] args) {
    int[] input = {6,8,3,5,9,2,4,7,1,9};
    quickSort(input, 0, input.length-1);
    for(int cur:input){
      System.out.print(cur+",");
    }
  }

  public static void quickSort(int[] arr, int start, int end) {
    if(start >= end) {
      return;
    }
    int pivot = partition(arr, start, end);
    quickSort(arr, start, pivot-1);
    quickSort(arr, pivot+1, end);
  }

  private static int partition(int[] arr, int start, int end) {
    int pivot = arr[end];
    int pIdx = start;

    for(int i=start; i<end; i++){
      if(arr[i] <= pivot) {
        if(i!=pIdx) {
          swap(arr, i, pIdx);
        }
        pIdx++;
      }
    }
    swap(arr, end, pIdx);
    return pIdx;
  }

  private static void swap(int[] arr, int i, int j){
    int temp = arr[i];
    arr[i] = arr[j];
    arr[j] = temp;
  }
}
