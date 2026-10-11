package io.github.legendarybosses;
final class BossRules {
 static double bounded(double value,double fallback,double min,double max){return Double.isFinite(value)?Math.clamp(value,min,max):fallback;}
 static double health(double hearts){return 2*bounded(hearts,100,1,500);}
 static boolean reward(boolean requirePlayerKill,boolean playerKill){return !requirePlayerKill||playerKill;}
 private BossRules(){}
}
