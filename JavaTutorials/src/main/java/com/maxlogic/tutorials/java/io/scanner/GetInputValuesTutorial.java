package com.maxlogic.tutorials.java.io.scanner;

import java.util.Arrays;
import java.util.Scanner;

public class GetInputValuesTutorial {

  public static void main(String[] args) {
    //Get values in input
    Scanner scanner = new Scanner(System.in);
    System.out.println("");

    System.out.print("Enter number of nodes :");
    int noOfNodes = scanner.nextInt();
    System.out.println("Number of nodes:[" + noOfNodes + "]");
    scanner.nextLine();

    boolean incorrectVal = true;
    String[] nodeVals = new String[0];
    while(incorrectVal) {
      System.out.print("Enter node values in sequence: ");
      String nodevalues = scanner.nextLine();
      nodeVals = nodevalues.split(" ");
      if (nodeVals.length == noOfNodes) {
        incorrectVal = false;
      } else {
        System.out.println("Incorrect nodes entered, Please enter "+nodevalues+" node values");
      }
    }
    System.out.println("Node values "+ Arrays.toString(nodeVals));

    System.out.print("Enter node colours in sequence: ");
    int i=0;
    int[] colourVals = new int[noOfNodes];
    while(i<noOfNodes){
      int colourVal = scanner.nextInt();
      if(colourVal != 0 && colourVal != 1) {
        System.out.print("Incorrect value, Please enter values again");
        continue;
      } else {
        colourVals[i] = colourVal;
        i++;
      }
    }
    System.out.println("Colour values ["+ Arrays.toString(colourVals)+"]");



  }
}
