import java.io.*;import java.util.*;
/* NG - kompakte Fassung von NeuralGuesser. Der Computer erraet deine Zahl (0..99).
   Das neuronale Netz (MLP 3-24-24-1, Backprop + Adam, alles von Hand) lernt die Rate-
   strategie bei jedem Start neu per REINFORCE im Selbstspiel. Es kennt keine binaere Suche:
   Eingabe = Spanne [lo,hi], Ausgabe = Anteil mu, Tipp = lo+mu*(hi-lo), Reward = -1 je Versuch. */
public class NG{
static final int M=99,MS=110,BA=64,IT=3000,EV=125;
static final double LR=.01,SA=.3,SB=.06;
static final Random R=new Random();
static final int[]Z={3,24,24,1};static final int L=3;
static double[][][]W,G,Mw,Vw;static double[][]b,g,Mb,Vb,a;static int T;
static PrintStream o;static BufferedReader in;

public static void main(String[]x)throws Exception{
o=new PrintStream(new FileOutputStream(FileDescriptor.out),true,"UTF-8");
in=new BufferedReader(new InputStreamReader(System.in,"UTF-8"));
init();double v0=ev(3000);
o.printf(Locale.ROOT,"NeuralGuesser - das Netz lernt raten (Selbstspiel, bei jedem Start neu)%n%n"
+"Frisches Netz: %.2f Versuche/Spiel%n%nTraining (%d x %d Selbstspiele)...%n"
+" Iter | sigma |   lr    | Versuche | mu[0,99] | bestes%n",v0,IT,BA);
train();double v1=ev(3000);
o.printf(Locale.ROOT,"%nVor dem Training : %5.2f Versuche/Spiel%nSelbst gelernt   : %5.2f"
+"%nBinäre Suche     : %5.2f (Optimum)%n%nGelernte Strategie (mu = Anteil der Spanne, 0.5 = Mitte):%n",v0,v1,bin());
for(int[]q:new int[][]{{0,99},{0,49},{50,99},{20,80},{37,43},{0,9},{90,99}})
o.printf(Locale.ROOT,"  [%2d,%2d]  mu=%.3f  ->  %d%n",q[0],q[1],f(q[0],q[1]),gs(q[0],q[1]));
o.println("\nDas grenzt schon fast an künstliche Intelligenz.\n");play();}

/* ---- Spiel ---- */
static void play()throws IOException{for(;;){
o.println("Denke dir eine Zahl zwischen 0 und "+M+" und drücke dann Enter.");
if(in.readLine()==null)break;
o.println("\nOk. Ich beginne jetzt zu raten.\nAntworte mir mit 1 für richtig, 2 für kleiner und 3 für grösser.\n");
int lo=0,hi=M,n=0;
for(;;){if(lo>hi){o.println("Hmm, deine Antworten passen nicht zusammen. Hast du geschummelt?");break;}
 int q=gs(lo,hi);n++;o.println("Ist es die "+q+"?");int c=0;
 while(c==0){String t=in.readLine();if(t==null)return;t=t.trim();
  if(t.equals("1")||t.equals("2")||t.equals("3"))c=t.charAt(0)-'0';
  else o.println("Bitte 1 (richtig), 2 (kleiner) oder 3 (grösser) eingeben.");}
 if(c==1){o.println((n<4?"Das war ja einfach!":n<6?"Gefunden!":n<7?"Geschafft.":"Uff, das war schwierig.")+" ("+n+(n==1?" Versuch)":" Versuche)"));break;}
 if(c==2)hi=q-1;else lo=q+1;}
o.println("\nSpielen wir nochmal? (j/n)");String s=in.readLine();if(s==null)break;
s=s.trim().toLowerCase(Locale.ROOT);if(!(s.isEmpty()||s.startsWith("j")||s.startsWith("y")))break;o.println();}
o.println("Bis zum nächsten Mal!");}

/* ---- Training: REINFORCE mit Baseline je Spannenbreite ---- */
static void train(){
double[]bs=new double[M+1],bc=new double[M+1];int cap=BA*MS+8;
int[]lo=new int[cap],hi=new int[cap];double[]ep=new double[cap],ad=new double[cap];
double[][][]BW=null;double[][]BB=null;double best=1e9;
for(int it=1;it<=IT;it++){
 double p=(it-1.)/(IT-1),sg=SA+(SB-SA)*p,lr=LR*.5*(1+Math.cos(Math.PI*p));int m=0,tot=0;
 for(int e=0;e<BA;e++){int sec=R.nextInt(M+1),l=0,h=M,st=m;
  for(;;){double mu=f(l,h),z=R.nextGaussian(),u=mu+sg*z;u=u<0?0:u>1?1:u;
   int q=l+(int)Math.round(u*(h-l));q=q<l?l:q>h?h:q;lo[m]=l;hi[m]=h;ep[m]=z;m++;
   if(q==sec)break;if(sec<q)h=q-1;else l=q+1;if(m-st>=MS)break;}
  int n=m-st;tot+=n;
  for(int i=0;i<n;i++){double gg=-(n-i);int wd=hi[st+i]-lo[st+i];
   ad[st+i]=gg-(bc[wd]>0?bs[wd]/bc[wd]:0);bs[wd]=bs[wd]*.995+gg;bc[wd]=bc[wd]*.995+1;}}
 double mn=0,vr=0;for(int i=0;i<m;i++)mn+=ad[i];mn/=m;
 for(int i=0;i<m;i++)vr+=(ad[i]-mn)*(ad[i]-mn);double sd=Math.sqrt(vr/m)+1e-8;
 for(int i=0;i<m;i++){f(lo[i],hi[i]);double d=-(ad[i]-mn)/sd*ep[i]/sg;bk(d>20?20:d<-20?-20:d);}
 upd(lr,m);boolean im=false;
 if(it%EV==0||it==IT){double s=ev(800);if(s<best){best=s;BW=c3(W);BB=c2(b);im=true;}}
 if(it==1||it%250==0)o.printf(Locale.ROOT," %4d |%.4f |%.6f |   %5.2f |   %.4f | %s%n",it,sg,lr,
  tot/(double)BA,f(0,M),best>1e8?"-":String.format(Locale.ROOT,"%.2f%s",best,im?" *":""));}
if(BW!=null){W=BW;b=BB;}}

/* ---- Auswertung ---- */
static double ev(int n){Random q=new Random(4711);long t=0;
for(int k=0;k<n;k++){int s=q.nextInt(M+1),l=0,h=M,c=0;
 for(;;){int p=gs(l,h);c++;if(p==s)break;if(s<p)h=p-1;else l=p+1;if(c>=MS)break;}t+=c;}return t/(double)n;}
static double bin(){long t=0;for(int s=0;s<=M;s++){int l=0,h=M,c=0;
 for(;;){int p=(l+h)/2;c++;if(p==s)break;if(s<p)h=p-1;else l=p+1;}t+=c;}return t/(M+1.);}
static int gs(int l,int h){int q=l+(int)Math.round(f(l,h)*(h-l));return q<l?l:q>h?h:q;}

/* ---- neuronales Netz ---- */
static void init(){W=new double[L][][];G=new double[L][][];Mw=new double[L][][];Vw=new double[L][][];
b=new double[L][];g=new double[L][];Mb=new double[L][];Vb=new double[L][];a=new double[L+1][];a[0]=new double[Z[0]];
for(int l=0;l<L;l++){int p=Z[l],q=Z[l+1];W[l]=new double[q][p];G[l]=new double[q][p];Mw[l]=new double[q][p];
 Vw[l]=new double[q][p];b[l]=new double[q];g[l]=new double[q];Mb[l]=new double[q];Vb[l]=new double[q];a[l+1]=new double[q];
 double s=Math.sqrt(2./(p+q));
 for(int i=0;i<q;i++){for(int j=0;j<p;j++)W[l][i][j]=R.nextGaussian()*s;b[l][i]=l==L-1?R.nextDouble()*4-2:0;}}}
static double f(int lo,int hi){a[0][0]=lo/99.;a[0][1]=hi/99.;a[0][2]=(hi-lo)/99.;
for(int l=0;l<L;l++){double[]u=a[l],v=a[l+1];
 for(int i=0;i<Z[l+1];i++){double s=b[l][i];double[]w=W[l][i];for(int j=0;j<Z[l];j++)s+=w[j]*u[j];
  v[i]=l<L-1?Math.tanh(s):1/(1+Math.exp(-s));}}return a[L][0];}
static void bk(double d){double[]dl={d*a[L][0]*(1-a[L][0])};
for(int l=L-1;l>=0;l--){for(int i=0;i<Z[l+1];i++){double e=dl[i];g[l][i]+=e;double[]q=G[l][i],u=a[l];
  for(int j=0;j<Z[l];j++)q[j]+=e*u[j];}
 if(l>0){double[]n=new double[Z[l]];for(int j=0;j<Z[l];j++){double s=0;
   for(int i=0;i<Z[l+1];i++)s+=W[l][i][j]*dl[i];double v=a[l][j];n[j]=s*(1-v*v);}dl=n;}}}
static void upd(double lr,int n){T++;double c1=1-Math.pow(.9,T),c2=1-Math.pow(.999,T);
for(int l=0;l<L;l++)for(int i=0;i<Z[l+1];i++){double q=g[l][i]/n;g[l][i]=0;
 Mb[l][i]=.9*Mb[l][i]+.1*q;Vb[l][i]=.999*Vb[l][i]+.001*q*q;
 b[l][i]-=lr*(Mb[l][i]/c1)/(Math.sqrt(Vb[l][i]/c2)+1e-8);
 for(int j=0;j<Z[l];j++){double w=G[l][i][j]/n;G[l][i][j]=0;
  Mw[l][i][j]=.9*Mw[l][i][j]+.1*w;Vw[l][i][j]=.999*Vw[l][i][j]+.001*w*w;
  W[l][i][j]-=lr*(Mw[l][i][j]/c1)/(Math.sqrt(Vw[l][i][j]/c2)+1e-8);}}}
static double[][][]c3(double[][][]x){double[][][]c=new double[x.length][][];
for(int i=0;i<x.length;i++)c[i]=c2(x[i]);return c;}
static double[][]c2(double[][]x){double[][]c=new double[x.length][];
for(int i=0;i<x.length;i++)c[i]=x[i].clone();return c;}}
