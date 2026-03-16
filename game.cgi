#!/usr/bin/perl 

# 記録ファイル名を指定
# あらかじめファイルは作成し、chmod 666 などを行う必要アリ
my $logfile = "../cgi-dat/game.dat";
my $UpdateKey = 1019;

# 入力のパースは CGI モジュールに任せる
use CGI;
my $q = new CGI;
my $max = 6;

# MIMEヘッダーを出力
print $q->header(-type=>'text/plain');

# ID が指定されていたら、$logfile を書き換える
# $logfile が game.dat、ID が 2 のときには、game.dat.2 になるので、
# あらかじめファイルを作成し、chmod 666 などを行っておく
if("" ne $q->param('id')){
  my $id = int($q->param('id'));
  $logfile = "$logfile.$id";
}

# ファイルをオープンする。失敗した場合はエラー表示を行う。
if(!open(STREAM,"+<$logfile")){
  print "Content-Type: text/plain\n\n";
  print "Couldn't open the file: $logfile";
  exit;
}

# チェックサムを計算する
my $chksum = $q->param('score') + $q->param('time');
my $name = $q->param('name');
for(my $c=0;$c<length($name);$c++){
  $chksum += ord(substr($name,$c));
}
$chksum %= $UpdateKey;
my $id = $q->param('id');
for(my $c=0;$c<$id;$c++){
  $chksum = ($chksum * $chksum) % $UpdateKey;
}

# 排他制御でファイル・ロックを行う
flock(STREAM,2);

# 配列形式で入力
my @sortdata = <STREAM>;

# 各行末尾の改行コードを削除
chop(@sortdata);

# 入力された変数scoreに値があれば、記録を修正し、ファイルに書き出す
# $ENV{'HTTP_USER_AGENT'}=~m/Java/ && 
if($chksum == $q->param('chksum') && $q->param('score') ne ""){
  # ランキングデータを更新する
  push(@sortdata,$q->param('score').",".$q->param('time').",".$q->param('name'));
  # ソートを行う
  # 最初は時間でソート
  @sortdata = sort{(split(',',$a))[1]<=>(split(',',$b))[1]}(@sortdata);
  # 次に得点でソート
  @sortdata = sort{(split(',',$b))[0]<=>(split(',',$a))[0]}(@sortdata);
  # ファイルを 0 バイトに切り詰める
  truncate(STREAM,0);
  # ファイル・ポインターを先頭に移動
  seek(STREAM,0,0);
  # 2×$maxのデータをファイルに出力する
  for(my $i=0;$i<2*$max && $i<@sortdata;$i++){
    print STREAM $sortdata[$i],"\n";
  }
}

# FLASHファイル等に記録を返す
# トップ$maxのみ
for(my $i=0;$i<$max;$i++){
  print $sortdata[$i],"\n";
}

# 最後にファイルをクローズする
close(STREAM);
