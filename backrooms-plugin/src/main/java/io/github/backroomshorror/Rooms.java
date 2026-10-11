package io.github.backroomshorror;
/** Deterministic connected room grid, including negative chunk coordinates. */
final class Rooms {
 static boolean wall(int x,int z){int a=Math.floorMod(x,8),b=Math.floorMod(z,8);return a==0&&b!=3&&b!=4||b==0&&a!=3&&a!=4;}
 static boolean lamp(int x,int z){return Math.floorMod(x,8)==4&&Math.floorMod(z,8)==4;}
 static boolean exit(int x,int z){return x==52&&z==52;}
 private Rooms(){}
}
