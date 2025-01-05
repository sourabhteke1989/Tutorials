package com.maxlogic.tutorials.java.io.scanner;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Scanner;
import org.apache.commons.lang3.tuple.Pair;

public class GetInputFromFile {

  /**
   *
   * input file is input.txt
   */
  public static void main(String[] args) throws FileNotFoundException {
    Scanner scanner = new Scanner(new FileInputStream("src/main/java/com/maxlogic/java/io/scanner/input.txt"));
    int noOfNodes = Integer.parseInt(scanner.nextLine());
    String[] arr = scanner.nextLine().split(" ");
    int[] nodeValues = Arrays.stream(arr).mapToInt(Integer::valueOf).toArray();
    arr = scanner.nextLine().split(" ");
    int[] colourValues = Arrays.stream(arr).mapToInt(Integer::valueOf).toArray();
    Pair[] edges = new Pair[noOfNodes-1];
    for(int i=0;i<noOfNodes-1;i++) {
      arr = scanner.nextLine().split(" ");
      edges[i] = Pair.of(Integer.valueOf(arr[0]), Integer.valueOf(arr[1]));
    }

    System.out.println("Number of nodes:["+noOfNodes+"]");
    System.out.println("Node values:"+Arrays.toString(nodeValues));
    System.out.println("Node colors:"+Arrays.toString(colourValues));
    System.out.println("Edges:");
    Arrays.stream(edges).forEach(System.out::println);
  }
}
