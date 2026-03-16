/*
 * 本プログラムの著作権は、uncorrelated（uncorrelated@yahoo.co.jp ）に属します。
 * 利用者の責任において、自由に改編・変更を行い、自由な目的に利用することを認めますが、
 * その際にどのような損害が発生したとしても、著作者である uncorrelated は責任を負いません。
 */
import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

class Cancel {
	private int left, top, right, bottom;
	public void setRect(int left, int top, int right, int bottom) {
		this.left = left;
		this.right = right;
		this.top = top;
		this.bottom = bottom;

	}
	public boolean isRect(int x, int y) {
		if (left <= x && x <= right && top <= y && y <= bottom)
			return true;
		return false;
	}
}

class HiScore implements Runnable {
	private TextField tf;
	private Button bt, cb;
	private Score[] ranking = new Score[0];
	private volatile String URL = null;
	private String ID;
	private long IntervalTime;
	private Thread thrd;

	public HiScore(String URL, String ID, long IntervalTime) {
		this.URL = URL;
		this.ID = ID;
		this.IntervalTime = IntervalTime;
		thrd = new Thread(this);
		thrd.start();
	}

	public Score[] getRanking() {
		synchronized (this) {
			return ranking;
		}
	}

	private Score[] getScore(String urlString) {
		try {
			Vector vctr = new Vector();
			URL url = new URL(urlString);
			BufferedReader bis =
				new BufferedReader(new InputStreamReader(url.openStream()));
			String input;
			while (null != (input = bis.readLine())) {
				Score ranking = new Score();
				int index = 0, findex = 0;
				if (0 > (index = input.indexOf(',', findex)))
					break;
				ranking.score =
					Integer.parseInt(input.substring(findex, index));
				findex = index + 1;
				if (0 > (index = input.indexOf(',', findex)))
					break;
				ranking.time = Long.parseLong(input.substring(findex, index));
				findex = index + 1;
				ranking.name = input.substring(findex);
				vctr.addElement(ranking);
			}
			bis.close();
			int size = vctr.size();
			Score[] tmp = new Score[size];
			for (int c = 0; c < size; c++) {
				tmp[c] = (Score) vctr.elementAt(c);
			}
			return tmp;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private Score[] sendScore(String urlString, Score score, int UpdateKey) {
		StringBuffer sb = new StringBuffer(urlString);
		sb.append("?score=");
		sb.append(score.score);
		sb.append("&time=");
		sb.append(score.time);
		sb.append("&name=");
		sb.append(escape(score.name));
		if (null != ID) {
			sb.append("&id=");
			sb.append(escape(ID));
		}
		sb.append("&chksum=");
		sb.append(getChkSum(score, UpdateKey));
		Score[] ranking = getScore(sb.toString());
		synchronized (this) {
			this.ranking = ranking;
		}
		return ranking;
	}

	// 悪戯防止用のチェックサムを計算
	private long getChkSum(Score score, int UpdateKey) {
		long chksum = 0;
		byte[] nchar = score.name.getBytes();
		for (int c = 0; c < nchar.length; c++) {
			chksum += nchar[c];
		}
		chksum += score.time;
		chksum += score.score;
		chksum %= UpdateKey;
		try {
			if (null != ID) {
				int n = Integer.parseInt(ID);
				for (int c = 0; c < n; c++)
					chksum = (chksum * chksum) % UpdateKey;
			}
		} catch (NumberFormatException ex) {
			ex.printStackTrace();
		}
		return chksum;
	}

	public Score[] sendScore(Score score, int UpdateKey) {
		return sendScore(URL, score, UpdateKey);
	}

	private static String escape(String str) {
		byte[] src = null;
		try {
			src = str.getBytes("Shift_JIS");
		} catch (UnsupportedEncodingException e) {
			src = str.getBytes();
		}
		byte[] des = new byte[src.length * 5];
		int n = 0;
		for (int c = 0; c < src.length; c++) {
			if (('a' <= src[c] && src[c] <= 'z')
				|| ('A' <= src[c] && src[c] <= 'Z')
				|| ('0' <= src[c] && src[c] <= '9')) {
				des[n++] = src[c];
			} else {
				byte[] hex = Integer.toHexString(src[c]).getBytes();
				des[n++] = '%';
				if (2 <= hex.length) {
					des[n++] = hex[0];
					des[n++] = hex[1];
				} else {
					des[n++] = '0';
					des[n++] = hex[0];
				}
			}
		}
		return new String(des, 0, n);
	}

	public void run() {
		while (thrd == Thread.currentThread()) {
			while (true) {
				if (null != URL) {
					StringBuffer sb = new StringBuffer(URL);
					if (null != ID) {
						sb.append("?id=");
						sb.append(escape(ID));
					}
					Score[] ranking = getScore(sb.toString());
					synchronized (this) {
						this.ranking = ranking;
					}
				}
				try {
					Thread.sleep(IntervalTime);
				} catch (InterruptedException e) {
				}
			}
		}
	}
}

class Score {
	public String name = "";
	public long time = 0;
	public int score = 0;

}

// タイマー
class Timer {
	private boolean flag = false;
	private int height = -1, width = -1;
	private Font monospace = new Font("Monospace", Font.BOLD, 12);
	private long start, stop;
	private Properties prop;
	private Dimension size;

	public Timer(Properties prop, Dimension dim) {
		this.prop = prop;
		setSize(dim);
	}

	public void setSize(Dimension dim) {
		size = dim;
	}

	private void addNumber(StringBuffer sb, long Number, int MaxNumber) {
		for (int c = MaxNumber; c > 0; c /= 10) {
			if (Number <= c)
				sb.append('0');
		}
		sb.append(Number);
	}

	public void draw(Graphics g) {
		g.setColor(Color.white);
		g.setFont(monospace);

		StringBuffer sb = new StringBuffer(prop.getProperty("StringTime"));
		sb.append(":");
		sb.append(getTimerString(get()));
		String timerString = new String(sb);

		FontMetrics fm = g.getFontMetrics();
		height = fm.getHeight();
		width = fm.stringWidth(timerString);

		g.setColor(Color.black);
		g.drawString(
			timerString,
			size.width - width - 3 + 1,
			size.height - 3 + 1);
		g.setColor(Color.orange);
		g.drawString(timerString, size.width - width - 3, size.height - 3);
	}

	public String getTimerString(long time) {
		StringBuffer sb = new StringBuffer();
		long min = time / (60*1000);
		time -= 60*1000*min;
		long sec = time / 1000;
		long msec = (time % 1000) / 10;
		addNumber(sb, min, 9);
		sb.append(":");
		addNumber(sb, sec, 9);
		sb.append(":");
		addNumber(sb, msec, 9);
		return new String(sb);
	}

	public long get() {
		if (flag)
			return (new Date()).getTime() - start;
		else
			return stop - start;
	}
	public void start() {
		start = new Date().getTime();
		flag = true;
	}
	public void stop() {
		stop = (new Date()).getTime();
		flag = false;
	}
	public void clear() {
		start = 0;
		stop = 0;
	}
}

public class Puzzle15
	extends Applet
	implements KeyListener, MouseListener, Runnable {
	private Image Image = null;
	private int Column, Row;
	private int NumberOfPanel;
	private int[][] Matrix = null; // 排他処理が必要
	private final int MovingNone = -1;
	private final int MovingLeft = 1;
	private final int MovingTop = 2;
	private final int MovingRight = 3;
	private final int MovingBottom = 4;
	private boolean[][] MovingPanel = null; // 排他処理が必要
	private int MovingX = 0;
	private int MovingY = 0;
	private int MovingLevel = 0;
	private final int MovingMaxLevel = 8;
	private int MovingDirection = MovingNone; // 排他処理が必要
	private int MovingWait = 20;
	private int EmptyPanel = -1;
	private Thread thrd;
	private Timer timer = null;
	private Properties P15Properties = new Properties();
	private final int GameIsReady = 1;
	private final int GameIsPlaying = 2;
	private final int GameIsEnd = 3;
	private final int GameIsRanking = 4;
	private final int GameIsHiScore = 5;
	private volatile int GameStatus = GameIsReady;
	private int NumberOfClick = 0;
	private int NumberOfShuffle = 64;
	private Font monospace = new Font("Monospace", Font.BOLD, 12);
	private AudioClip SoundMove = null;
	private AudioClip SoundEnd = null;
	private int ClickPenalty = 10;
	private int TimePenalty = 1;
	private int BaseScore;
	// 初期画面（GameIsReady）での経過時間カウンター
	private long TimeToChangeHiScore = 5000; // 必要経過時間・ミリ秒
	private long TimeToReloadHiScore = 60000; // リロード必要経過時間・ミリ秒
	private long TimeToChangeHiScoreStart = new Date().getTime();
	// エンディング画面での画面切替
	private long DrawLastUpdate;
	private int DrawPtr = 0;
	private long DrawInterval = 5000;
	// ハイスコア処理用
	private char[] input = new char[20];
	private int input_ptr = 0;
	private HiScore hiscore = null;
	private int MaximumRankingNumber = 5;
	private int UpdateKey = 0xffff;
	// キャンセル・ボタンの位置
	private Cancel cancel = new Cancel();
	private int DebugLevel = 0;

	public void init() {
		try {
			P15Properties.load(
				Puzzle15.class.getResourceAsStream("p15.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		SoundMove =
			getAudioClip(getCodeBase(), P15Properties.getProperty("SoundMove"));
		SoundEnd =
			getAudioClip(getCodeBase(), P15Properties.getProperty("SoundEnd"));
		ClickPenalty =
			Integer.parseInt(P15Properties.getProperty("ScoreClickPenalty"));
		TimePenalty =
			Integer.parseInt(P15Properties.getProperty("ScoreTimePenalty"));

		try {
			Column = Integer.parseInt(getParameter("Column"));
		} catch (Exception e) {
			Column = Integer.parseInt(P15Properties.getProperty("Column"));
		}
		try {
			Row = Integer.parseInt(getParameter("Row"));
		} catch (Exception e) {
			Row = Integer.parseInt(P15Properties.getProperty("Row"));
		}
		try {
			BaseScore = Integer.parseInt(getParameter("BaseScore"));
		} catch (Exception e) {
			BaseScore = Integer.parseInt(P15Properties.getProperty("ScoreBase"));
		}

		Image = getImage(getCodeBase(), getParameter("Image"));
		Matrix = new int[Row][Column];
		MovingPanel = new boolean[Row][Column];
		NumberOfPanel = Row * Column;
		EmptyPanel = NumberOfPanel - 1;
		NumberOfShuffle = NumberOfPanel * NumberOfPanel;

		TimeToReloadHiScore =
			1000
				* Integer.parseInt(
					P15Properties.getProperty("TimeToReloadHiScore"));
		TimeToChangeHiScore =
			1000
				* Integer.parseInt(
					P15Properties.getProperty("TimeToChangeHiScore"));
		String URL = getParameter("HiScoreURL");
		UpdateKey = Integer.parseInt(P15Properties.getProperty("UpdateKey"));
		if (null != URL) {
			hiscore =
				new HiScore(
					URL,
					getParameter("HiScoreID"),
					TimeToReloadHiScore);
		}
		DebugLevel = Integer.parseInt(P15Properties.getProperty("DebugLevel"));

		readyToGame();
		addMouseListener(this);
		addKeyListener(this); // ランキングの名前入力用にキーリスナーを設定
		thrd = new Thread(this);
		thrd.start();
	}

	private void readyToGame() {
		int n = 0;
		for (int y = 0; y < Matrix.length; y++) {
			for (int x = 0; x < Matrix[y].length; x++) {
				Matrix[y][x] = n++;
			}
		}
		int x1 = Column - 1;
		int y1 = Row - 1;
		int bx = 0;
		int by = 0;
		int[] dx = { -1, 0, 1, 0 };
		int[] dy = { 0, -1, 0, 1 };
		for (int c = 0; c < NumberOfShuffle; c++) {
			int dice = (int) (4 * Math.random());
			int x2 = x1 + dx[dice];
			int y2 = y1 + dy[dice];
			if (x2 < 0 || x2 >= Column)
				continue;
			if (y2 < 0 || y2 >= Row)
				continue;
			if (x2 == bx && y2 == by)
				continue;
			if (DebugLevel == 1) {
				System.out.print("(" + x1 + "," + y1 + ")");
				System.out.print(" ---> ");
				System.out.println("(" + x2 + "," + y2 + ")");
			}
			bx = x1;
			by = y1;
			int tmp = Matrix[y1][x1];
			Matrix[y1][x1] = Matrix[y2][x2];
			Matrix[y2][x2] = tmp;
			x1 = x2;
			y1 = y2;
		}
		NumberOfClick = 0;
		TimeToChangeHiScoreStart = new Date().getTime();
		if (null != timer)
			timer.clear();
		GameStatus = GameIsReady;
	}

	public void destroy() {
	}

	public void update(Graphics g) {
		paint(g);
	}

	private Image ShadowBuffer = null;
	private boolean isInit = false;

	private void initP15(
		Graphics g,
		Dimension size) {

		if (isInit)
			return;

		ShadowBuffer = createImage(size.width, size.height);
		timer = new Timer(P15Properties, size);
		isInit = true;
	}

	private void drawReady(
		Graphics g,
		Dimension size,
		int ImageWidth,
		int ImageHeight) {
		g.drawImage(Image, 0, 0, this);
		String readyString = P15Properties.getProperty("StringStart");
		g.setFont(monospace);
		FontMetrics fm = g.getFontMetrics();
		int height = fm.getHeight();
		int width = fm.stringWidth(readyString);
		g.setColor(Color.yellow);
		drawText(g, fm, size, (size.height - height) / 2, readyString);
	}

	private void drawPlaying(
		Graphics dg,
		Dimension size,
		int ImageWidth,
		int ImageHeight) {

		dg.setColor(Color.gray);
		dg.fillRect(0, 0, size.width, size.height);

		int EachImageHeight = ImageHeight / Row;
		int EachImageWidth = ImageWidth / Column;
		int ExtraImageWidth = ImageWidth - EachImageWidth * Column;
		int ExtraImageHeight = ImageHeight - EachImageHeight * Row;

		for (int y = 0; y < Matrix.length; y++) {
			for (int x = 0; x < Matrix[y].length; x++) {
				int n = Matrix[y][x];
				if (n != EmptyPanel) {
					int iy = n / Column;
					int ix = n % Column;
					int wx = EachImageWidth * x;
					int wy = EachImageHeight * y;

					if (MovingPanel[y][x]) {
						switch (MovingDirection) {
							case MovingLeft :
								wx -= EachImageWidth
									* MovingLevel
									/ MovingMaxLevel;
								break;
							case MovingTop :
								wy -= EachImageHeight
									* MovingLevel
									/ MovingMaxLevel;
								break;
							case MovingRight :
								wx += EachImageWidth
									* MovingLevel
									/ MovingMaxLevel;
								break;
							case MovingBottom :
								wy += EachImageHeight
									* MovingLevel
									/ MovingMaxLevel;
								break;
						}
					}

					int ExtraWidth = x == Column - 1 ? ExtraImageWidth : 0;
					int ExtraHeight = y == Row - 1 ? ExtraImageHeight : 0;

					dg.drawImage(
						Image,
						wx,
						wy,
						wx + EachImageWidth + ExtraWidth,
						wy + EachImageHeight + ExtraHeight,
						ix * EachImageWidth,
						iy * EachImageHeight,
						(ix + 1) * EachImageWidth + ExtraWidth,
						(iy + 1) * EachImageHeight + ExtraHeight,
						this);
				}
			}
		}
	}

	private void drawEnd(
		Graphics g,
		Dimension size,
		int ImageWidth,
		int ImageHeight) {
		g.drawImage(Image, 0, 0, this);
		String EndString = P15Properties.getProperty("StringEnd");
		String ClickString = P15Properties.getProperty("StringClick");
		String TimeString = P15Properties.getProperty("StringTime");
		String TotalString = P15Properties.getProperty("StringTotal");
		g.setFont(monospace);
		FontMetrics fm = g.getFontMetrics();
		int height = fm.getHeight();
		int width;
		int py = (size.height - height) / 2 - 4 * height;
		g.setColor(Color.yellow);
		drawText(g, fm, size, py, EndString);
		py += height + 4;
		g.setColor(Color.yellow);
		drawLeft(g, size, ClickString, py);
		g.setColor(Color.green);
		drawRight(g, size, Integer.toString(NumberOfClick), fm, py);
		py += height + 4;
		g.setColor(Color.yellow);
		drawLeft(g, size, TimeString, py);
		g.setColor(Color.green);
		drawRight(g, size, timer.getTimerString(timer.get()), fm, py);
		py += height + 4;
		g.setColor(Color.yellow);
		drawLeft(g, size, TotalString, py);
		int total = calcScore();
		g.setColor(Color.green);
		drawRight(g, size, Integer.toString(total), fm, py);
		g.setColor(Color.yellow);
		g.drawLine(4, py - height - 1, size.width - 4, py - height - 1);
		py += height + 4;
		drawCancel(g, fm, size, py, "StringScoreEscape");
	}

	private int calcScore() {
		return BaseScore
			- (int) (timer.get() / 1000) * TimePenalty
			- NumberOfClick * ClickPenalty;
	}

	private void drawRight(
		Graphics g,
		Dimension size,
		String string,
		FontMetrics fm,
		int y) {
		int width = fm.stringWidth(string);
		Color color = g.getColor();
		g.setColor(Color.black);
		g.drawString(string, size.width - width - 8 + 1, y + 1);
		g.setColor(color);
		g.drawString(string, size.width - width - 8, y);
	}

	private void drawLeft(Graphics g, Dimension size, String string, int y) {
		Color color = g.getColor();
		g.setColor(Color.black);
		g.drawString(string, 8 + 1, y + 1);
		g.setColor(color);
		g.drawString(string, 8, y);
	}

	private void drawText(
		Graphics g,
		FontMetrics fm,
		Dimension dim,
		int py,
		String text) {
		int width = fm.stringWidth(text);
		Color color = g.getColor();
		g.setColor(Color.black);
		g.drawString(text, (dim.width - width) / 2 + 1, py + 2);
		g.setColor(color);
		g.drawString(text, (dim.width - width) / 2, py);
	}

	private void drawPropertyName(
		Graphics g,
		FontMetrics fm,
		Dimension dim,
		int py,
		String propertyName) {
		drawText(g, fm, dim, py, P15Properties.getProperty(propertyName));
	}

	private void drawScore(
		Graphics g,
		FontMetrics fm,
		Dimension dim,
		int py,
		int rank,
		String name,
		long time,
		int number) {
		// まず順位
		int height = fm.getHeight();
		int width;
		String rankString = Integer.toString(rank);
		width = fm.stringWidth(rankString);
		g.setColor(Color.black);
		g.drawString(
			rankString,
			dim.width / 12 - width + 1,
			py + 1);
		g.setColor(Color.yellow);
		g.drawString(
			rankString,
			dim.width / 12 - width,
			py);

		// 左側に名前
		width = fm.stringWidth(name);
		g.setColor(Color.black);
		g.drawString(name, dim.width / 8 + 1, py + 1);
		g.setColor(Color.yellow);
		g.drawString(name, dim.width / 8, py);

		// 中央に時間
		String timeString = timer.getTimerString(time);
		width = fm.stringWidth(timeString);
		g.setColor(Color.black);
		g.drawString(
			timeString,
			dim.width - 2 * dim.width / 8 - width + 1,
			py + 1);
		g.setColor(0 < number ? Color.green : Color.red);
		g.drawString(timeString, dim.width - 2 * dim.width / 8 - width, py);

		// 右側に数字
		String numberString = Integer.toString(number);
		width = fm.stringWidth(numberString);
		g.setColor(Color.black);
		g.drawString(
			numberString,
			dim.width - dim.width / 16 - width + 1,
			py + 1);
		g.setColor(0 < number ? Color.green : Color.red);
		g.drawString(numberString, dim.width - dim.width / 16 - width, py);

	}

	private int drawHiScore(
		Graphics g,
		FontMetrics fm,
		Dimension size,
		int MaxLength,
		int height,
		int start_y,
		Score[] ranking) {
		if (0 == ranking.length)
			return 1;
		int y = 0;
		g.setColor(Color.yellow);
		drawPropertyName(g, fm, size, start_y + y++ * height, "StringRanking");

		// 一定時間で表示するランキング位置を切り替える
		long thisTime = new Date().getTime();
		if (thisTime >= DrawInterval + DrawLastUpdate) {
			DrawLastUpdate = thisTime;
			DrawPtr += MaxLength;
			if (ranking.length <= DrawPtr)
				DrawPtr = 0;
		}

		int n;
		for (int c = 0;
			c < MaxLength && (n = (DrawPtr + c)) < ranking.length;
			c++) {
			drawScore(
				g,
				fm,
				size,
				start_y + y++ * height,
				n + 1,
				ranking[n].name,
				ranking[n].time,
				ranking[n].score);
		}

		y++;
		return y;
	}

	private int calcMaxLine(
		Dimension size,
		int data_size,
		int height,
		int line) {
		// 利用メッセージを表示する最大数を、画面サイズより計算
		int tmp = size.height / height - line;
		if (tmp <= 0)
			tmp = 1;
		return data_size <= tmp ? data_size : tmp;
	}

	private void drawHiScore(Graphics g, Dimension size) {
		g.drawImage(Image, 0, 0, this);
		if (null == hiscore)
			return;

		// HiSocreが内部的に一定時間でリロード
		Score[] ranking = hiscore.getRanking();

		FontMetrics fm = g.getFontMetrics();
		int height = fm.getHeight();
		int y = 0;
		int MaxLength = calcMaxLine(size, ranking.length, height, 10);
		int line = 3 + MaxLength;
		int TextHeight = line * height;

		y
			+= drawHiScore(
				g,
				fm,
				size,
				MaxLength,
				height,
				(size.height - TextHeight) / 2 + y * height,
				ranking);

		y++;
		g.setColor(Color.red);
		drawCancel(
			g,
			fm,
			size,
			(size.height - TextHeight) / 2 + y++ * height,
			"StringHiScoreEscape");
	}

	private void writeHiScore() {
		if (null != hiscore) {
			Score score = new Score();
			score.score = calcScore();
			score.time = timer.get();
			score.name = new String(input, 0, input_ptr);
			hiscore.sendScore(score, UpdateKey);
		}
		readyToGame();
	}

	private void drawCancel(
		Graphics g,
		FontMetrics fm,
		Dimension dim,
		int py,
		String propertyName) {
		String text = P15Properties.getProperty(propertyName);
		int width = fm.stringWidth(text) + 4;
		int height = fm.getHeight();
		int px = (dim.width - width) / 2;
		int left = px - 1 - 4;
		int top = py - height;
		g.setColor(Color.black);
		g.drawRect(left + 1, top + 1, width + 8, height + 6);
		g.drawString(text, px + 1, py + 1);
		g.setColor(Color.red);
		g.drawRect(left, top, width + 8, height + 6);
		g.drawString(text, px, py);
		cancel.setRect(left, top, left + width + 8, top + height + 6);
	}

	private void drawClick(Graphics g, Dimension size) {
		String ClickString = P15Properties.getProperty("StringClick");
		g.setFont(monospace);
		FontMetrics fm = g.getFontMetrics();
		String temp = ClickString + ":" + NumberOfClick;
		g.setColor(Color.black);
		g.drawString(temp, 3 + 1, size.height - 3 + 1);
		g.setColor(Color.orange);
		g.drawString(temp, 3, size.height - 3);

	}

	private void drawRanking(Graphics g, Dimension size) {
		g.drawImage(Image, 0, 0, this);
		if (null == hiscore)
			return;

		Score[] ranking = hiscore.getRanking();

		FontMetrics fm = g.getFontMetrics();
		int height = fm.getHeight();
		int MaxLength = calcMaxLine(size, ranking.length, height, 10);
		int line = 4 + MaxLength;
		int TextHeight = line * height;
		int y = 0;
		boolean isEntry = false;

		g.setFont(monospace);
		if (MaximumRankingNumber > ranking.length
			|| ranking[ranking.length - 1].score < calcScore()) {
			line += 3;
			isEntry = true;
			g.setColor(Color.yellow);
			drawPropertyName(
				g,
				fm,
				size,
				(size.height - TextHeight) / 2 + y++ * height,
				"StringRankingMessage1");
			drawPropertyName(
				g,
				fm,
				size,
				(size.height - TextHeight) / 2 + y++ * height,
				"StringRankingMessage2");

			g.setColor(Color.pink);
			drawText(
				g,
				fm,
				size,
				(size.height - TextHeight) / 2 + y++ * height,
				new String(input, 0, input_ptr));
		}

		y
			+= drawHiScore(
				g,
				fm,
				size,
				MaxLength,
				height,
				(size.height - TextHeight) / 2 + y * height,
				ranking);

		g.setColor(Color.red);
		drawCancel(
			g,
			fm,
			size,
			(size.height - TextHeight) / 2 + y++ * height,
			isEntry ? "StringRankingEscape" : "StringRankingNext");
	}

	private void drawLoadError(Graphics g,Dimension size){
		FontMetrics fm = g.getFontMetrics();
		g.setColor(Color.gray);
		g.fillRect(0,0,size.width,size.height);
		g.setColor(Color.yellow);
		int height = fm.getHeight();
		String fname = getParameter("Image");
		int y = (size.height - 2 * height)/2;
		if(null==fname){
			drawLeft(g,size,"The parameter is not set:",y);
			drawLeft(g,size,"Image",y + height);
		}
		else{
			drawLeft(g,size,"Now loading the file:",y);
			drawLeft(g,size,fname,y + height);
		}
	}

	public void paint(Graphics g) {
		Dimension size = getSize();
		initP15(g, size);
		Graphics dg = ShadowBuffer.getGraphics();

		// 画像ファイルがロードできないときの処理
		int ImageHeight=-1,ImageWidth=-1;
		try{
			ImageHeight = Image.getHeight(this);
			ImageWidth = Image.getWidth(this);
		}catch(NullPointerException ex){
			drawLoadError(dg,size);
			g.drawImage(ShadowBuffer, 0, 0, this);
			return;
		}
		if (ImageHeight <= 0 || ImageWidth <= 0){
			drawLoadError(dg,size);
			g.drawImage(ShadowBuffer, 0, 0, this);
			return;
		}

		switch (GameStatus) {
			case GameIsReady :
				drawReady(dg, size, ImageWidth, ImageHeight);
				break;
			case GameIsPlaying :
				drawPlaying(dg, size, ImageWidth, ImageHeight);
				drawClick(dg, size);
				timer.draw(dg);
				break;
			case GameIsEnd :
				drawEnd(dg, size, ImageWidth, ImageHeight);
				break;
			case GameIsRanking :
				drawRanking(dg, size);
				break;
			case GameIsHiScore :
				drawHiScore(dg, size);
				break;
		}
		g.drawImage(ShadowBuffer, 0, 0, this);
	}

	public void start() {
	}
	public void stop() {
	}

	private boolean isCompleted() {
		int n = 0;
		for (int y = 0; y < Matrix.length; y++) {
			for (int x = 0; x < Matrix[y].length; x++) {
				if (Matrix[y][x] != n++)
					return false;
			}
		}
		return true;
	}

	private void movePanel(MouseEvent e) {
		if(null==Image)
			return;

		int cx = e.getX() * Column / Image.getWidth(this);
		int cy = e.getY() * Row / Image.getHeight(this);

		if (cx < 0 || cy < 0)
			return;
		if (cx >= Column || cy >= Row)
			return;
		if (EmptyPanel == Matrix[cy][cx])
			return;

		synchronized (this) {
			if (MovingDirection == MovingNone && cx < Column && cy < Row) {
				MovingX = cx;
				MovingY = cy;
				for (int x = cx - 1; x >= 0; x--) {
					if (EmptyPanel == Matrix[cy][x]) {
						for (int mx = x + 1; mx < cx; mx++) {
							MovingPanel[cy][mx] = true;
						}
						MovingDirection = MovingLeft;
						NumberOfClick++;
						SoundMove.play();
						MovingPanel[cy][cx] = true;
						return;
					}
				}
				for (int y = cy - 1; y >= 0; y--) {
					if (EmptyPanel == Matrix[y][cx]) {
						if (0 < DebugLevel) {
							System.out.println("mx:" + cx + " my:" + y);
						}
						for (int my = y + 1; my < cy; my++) {
							MovingPanel[my][cx] = true;
						}
						MovingDirection = MovingTop;
						NumberOfClick++;
						SoundMove.play();
						MovingPanel[cy][cx] = true;
						return;
					}
				}
				for (int x = cx + 1; x < Column; x++) {
					if (EmptyPanel == Matrix[cy][x]) {
						for (int mx = x - 1; mx > cx; mx--) {
							MovingPanel[cy][mx] = true;
						}
						MovingDirection = MovingRight;
						NumberOfClick++;
						SoundMove.play();
						MovingPanel[cy][cx] = true;
						return;
					}
				}
				for (int y = cy + 1; y < Row; y++) {
					if (EmptyPanel == Matrix[y][cx]) {
						for (int my = y - 1; my > cy; my--) {
							MovingPanel[my][cx] = true;
						}
						MovingDirection = MovingBottom;
						NumberOfClick++;
						SoundMove.play();
						MovingPanel[cy][cx] = true;
						return;
					}
				}
			}
		}
	}

	private void readyToRanking() {
		GameStatus = GameIsRanking;
		DrawPtr = 0;
		DrawLastUpdate = new Date().getTime();
	}

	public void mouseClicked(MouseEvent e) {
		switch (GameStatus) {
			case GameIsReady :
				GameStatus = GameIsPlaying;
				if(null!=timer)
					timer.start();
				break;
			case GameIsPlaying :
				movePanel(e);
				debug();
				break;
			case GameIsEnd :
				if (cancel.isRect(e.getX(), e.getY())) {
					if (null == hiscore)
						readyToGame();
					else
						readyToRanking();
				}
				break;
			case GameIsRanking :
				if (cancel.isRect(e.getX(), e.getY()))
					readyToGame();
				break;
			case GameIsHiScore :
				if (cancel.isRect(e.getX(), e.getY()))
					readyToGame();
				break;
		}
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
	}

	public void mouseReleased(MouseEvent e) {
	}
	private void readyToHiScore() {
		GameStatus = GameIsHiScore;
		DrawPtr = 0;
		DrawLastUpdate = new Date().getTime();
	}

	private void debug() {
		if (0 == DebugLevel)
			return;
		System.out.println("MovingDirection:" + MovingDirection);
		System.out.println("MovingPanel:");
		for (int y = 0; y < MovingPanel.length; y++) {
			for (int x = 0; x < MovingPanel[y].length; x++) {
				System.out.print(MovingPanel[y][x] + " ");
			}
			System.out.println();
		}
		System.out.println("Matrix:");
		for (int y = 0; y < Matrix.length; y++) {
			for (int x = 0; x < Matrix[y].length; x++) {
				System.out.print(Matrix[y][x] + " ");
			}
			System.out.println();
		}
	}

	private void ThreadPlaying() {
		synchronized (this) {
			if (MovingDirection != MovingNone) {
				if (++MovingLevel >= MovingMaxLevel) {
					switch (MovingDirection) {
						case MovingLeft :
							for (int x = 1; x < Column; x++) {
								if (MovingPanel[MovingY][x]) {
									Matrix[MovingY][x - 1] = Matrix[MovingY][x];
								}
								MovingPanel[MovingY][x] = false;
							}
							MovingPanel[MovingY][0] = false;
							break;
						case MovingTop :
							for (int y = 1; y < Row; y++) {
								if (MovingPanel[y][MovingX]) {
									MovingPanel[y][MovingX] = false;
									Matrix[y - 1][MovingX] = Matrix[y][MovingX];
								}
								MovingPanel[y][MovingX] = false;
							}
							MovingPanel[0][MovingX] = false;
							break;
						case MovingRight :
							for (int x = Column - 1; x > 0; x--) {
								if (MovingPanel[MovingY][x - 1]) {
									Matrix[MovingY][x] = Matrix[MovingY][x - 1];
								}
								MovingPanel[MovingY][x] = false;
							}
							MovingPanel[MovingY][0] = false;
							break;
						case MovingBottom :
							for (int y = Row - 1; y > 0; y--) {
								if (MovingPanel[y - 1][MovingX]) {
									Matrix[y][MovingX] = Matrix[y - 1][MovingX];
								}
								MovingPanel[y][MovingX] = false;
							}
							MovingPanel[0][MovingX] = false;
							break;
					}
					Matrix[MovingY][MovingX] = EmptyPanel;
					MovingDirection = MovingNone;
					MovingLevel = 0;
					if (0 < DebugLevel) {
						for (int y = 0; y < MovingPanel.length; y++) {
							for (int x = 0; x < MovingPanel[y].length; x++) {
								if (MovingPanel[y][x]) {
									System.out.println(
										"MovingPanel に true があります");
									debug();
								}
							}
						}
						for (int y = 0; y < MovingPanel.length; y++) {
							for (int x = 0; x < MovingPanel[y].length; x++) {
								MovingPanel[y][x] = false;
							}
						}
					}
					if (isCompleted()) {
						GameStatus = GameIsEnd;
						SoundEnd.play();
						timer.stop();
					}
				}
			}
		}
	}

	private void ThreadReady() {
		// 一定時間が経過したら、ハイスコア表示画面に切り替え
		// ハイスコアが有効時のみ
		if (null != hiscore && GameIsReady == GameStatus) {
			if (new Date().getTime()
				> TimeToChangeHiScoreStart + TimeToChangeHiScore)
				readyToHiScore();
		}
	}

	public void run() {
		while (thrd == Thread.currentThread()) {
			while (true) {
				switch (GameStatus) {
					case GameIsPlaying :
						ThreadPlaying();
						break;
					case GameIsReady :
						ThreadReady();
						break;
				}
				repaint();
				try {
					Thread.sleep(MovingWait);
				} catch (InterruptedException ex) {
				}
			}
		}
	}

	public void keyPressed(KeyEvent e) {
	}

	public void keyReleased(KeyEvent e) {

	}

	public void keyTyped(KeyEvent ev) {
		char ichar = ev.getKeyChar();
		if (ichar == KeyEvent.VK_BACK_SPACE || ichar == KeyEvent.VK_DELETE) {
			if (0 < input_ptr)
				input_ptr--;
			ev.consume();
		} else if (ichar == KeyEvent.VK_ENTER) {
			if (GameIsRanking == GameStatus)
				writeHiScore();
		} else {
			if (input.length > input_ptr) {
				input[input_ptr++] = ev.getKeyChar();
			}
		}
		if (GameIsRanking == GameStatus)
			repaint();
	}
}
