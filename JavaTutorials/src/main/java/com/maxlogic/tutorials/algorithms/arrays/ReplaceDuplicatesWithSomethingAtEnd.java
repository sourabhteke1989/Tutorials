package com.maxlogic.tutorials.algorithms.arrays;

public class ReplaceDuplicatesWithSomethingAtEnd {

  public static void main(String[] args) {

    int [] input = {1,2,2,2,4,4,4,5,6,7,33,44,55,55};

    /*
     *  Remove all duplicate numbers from array and add those many -1 at the end of array.
     *  input : [1,2,2,2,4,4,4,5,6,7,33,44,55,55]
     *  output : [1,2,4,5,6,7,33,44,55,-1,-1,-1,-1,-1]
     */

    int j=0;  
    for(int i=1;i<input.length;i++){
      if(input[j] == input[i]) {
        input[i] = -1;
      } else {
        if(j!=0) {
          int temp = input[i];
          input[i] = input[j+1];
          input[j+1] = temp;
        }
        j++;
      }
    }

    for (int k : input) {
      System.out.print(k + ",");
    }
  }

}
