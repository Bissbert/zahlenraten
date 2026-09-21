import java.io.*;import java.util.*;
/* NG - compact edition of NeuralGuesser. The computer guesses your number (0..99).
   The neural net (MLP 3-24-24-1, backprop + Adam, all by hand) relearns the guessing
   strategy on every start via REINFORCE in self-play. It knows nothing about binary search:
   input = span [lo,hi], output = fraction mu, guess = lo+mu*(hi-lo), reward = -1 per guess. */
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
o.printf(Locale.ROOT,"NeuralGuesser - the network learns to guess (self-play, relearned every start)%n%n"
+"Fresh network: %.2f guesses/game%n%nTraining (%d x %d self-play games)...%n"
+" Iter | sigma |   lr    |  guesses | mu[0,99] |   best%n",v0,IT,BA);
train();double v1=ev(3000);
o.printf(Locale.ROOT,"%nBefore training : %5.2f guesses/game%nSelf-taught     : %5.2f"
+"%nBinary search   : %5.2f (optimum)%n%nLearned strategy (mu = fraction of the span, 0.5 = midpoint):%n",v0,v1,bin());
for(int[]q:new int[][]{{0,99},{0,49},{50,99},{20,80},{37,43},{0,9},{90,99}})
o.printf(Locale.ROOT,"  [%2d,%2d]  mu=%.3f  ->  %d%n",q[0],q[1],f(q[0],q[1]),gs(q[0],q[1]));
o.println("\nThat's almost artificial intelligence.\n");play();}

/* ---- game ---- */
static void play()throws IOException{for(;;){
o.println("Think of a number between 0 and "+M+", then press Enter.");
if(in.readLine()==null)break;
o.println("\nOk. I'll start guessing now.\nAnswer me with 1 for correct, 2 for smaller and 3 for larger.\n");
int lo=0,hi=M,n=0;
for(;;){if(lo>hi){o.println("Hmm, your answers don't add up. Did you cheat?");break;}
 int q=gs(lo,hi);n++;o.println("Is it "+q+"?");int c=0;
 while(c==0){String t=in.readLine();if(t==null)return;t=t.trim();
  if(t.equals("1")||t.equals("2")||t.equals("3"))c=t.charAt(0)-'0';
  else o.println("Please enter 1 (correct), 2 (smaller) or 3 (larger).");}
 if(c==1){o.println((n<4?"Well, that was easy!":n<6?"Found it!":n<7?"Got it.":"Phew, that was tricky.")+" ("+n+(n==1?" guess)":" guesses)"));break;}
 if(c==2)hi=q-1;else lo=q+1;}
o.println("\nShall we play again? (y/n)");String s=in.readLine();if(s==null)break;
s=s.trim().toLowerCase(Locale.ROOT);if(!(s.isEmpty()||s.startsWith("y")||s.startsWith("j")))break;o.println();}
o.println("See you next time!");}

/* ---- training: REINFORCE with one baseline per span width ---- */
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

/* ---- evaluation ---- */
static double ev(int n){Random q=new Random(4711);long t=0;
for(int k=0;k<n;k++){int s=q.nextInt(M+1),l=0,h=M,c=0;
 for(;;){int p=gs(l,h);c++;if(p==s)break;if(s<p)h=p-1;else l=p+1;if(c>=MS)break;}t+=c;}return t/(double)n;}
static double bin(){long t=0;for(int s=0;s<=M;s++){int l=0,h=M,c=0;
 for(;;){int p=(l+h)/2;c++;if(p==s)break;if(s<p)h=p-1;else l=p+1;}t+=c;}return t/(M+1.);}
static int gs(int l,int h){int q=l+(int)Math.round(f(l,h)*(h-l));return q<l?l:q>h?h:q;}

/* ---- neural network ---- */
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
