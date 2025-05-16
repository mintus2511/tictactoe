import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.Random;
import java.util.Stack;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

// Define PowerUpType and PowerUp classes below if not already present
enum PowerUpType {
	EXTRA_TURN,
	REMOVE_OPPONENT_MARK
}

class PowerUp {
	PowerUpType type;
	java.awt.Point position;

	PowerUp(PowerUpType type, java.awt.Point position) {
		this.type = type;
		this.position = position;
	}
}

public class TicTacToe implements ActionListener{
	boolean gameOver = false;
	Random random = new Random();
	JFrame frame = new JFrame();
	JPanel title_panel = new JPanel();
	JPanel button_panel = new JPanel();
	JLabel textfield = new JLabel();
	JButton[] buttons = new JButton[9];
	boolean player1_turn;
	Stack<GameState> undoStack = new Stack<>();
	Stack<GameState> redoStack = new Stack<>();
    JButton undoButton = new JButton("Undo");
    JButton redoButton = new JButton("Redo");
	JButton restartButton = new JButton("Restart");
	private PowerUpType activePowerUp = null;
	private JButton powerUpButton = new JButton("Use Power-Up");
	private PowerUp currentPowerUp = null;
	private boolean powerUpOwnedByPlayer1 = false;
	private LinkedList<Integer> moveOrder = new LinkedList<>(); // lưu chỉ số nút theo thứ tự đã đánh
	private boolean extraTurnQueued = false;  // chờ lượt tiếp theo
	private boolean skipNextTurnSwitch = false;  // dùng sau khi nhấn EXTRA_TURN
	
	TicTacToe(){
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(800,800);
		frame.getContentPane().setBackground(new Color(50,50,50));
		frame.setLayout(new BorderLayout());
		frame.setVisible(true);
		
		textfield.setBackground(new Color(25,25,25));
		textfield.setForeground(new Color(25,255,0));
		textfield.setFont(new Font("Ink Free",Font.BOLD,75));
		textfield.setHorizontalAlignment(JLabel.CENTER);
		textfield.setText("Tic-Tac-Toe");
		textfield.setOpaque(true);
		
		title_panel.setLayout(new BorderLayout());
		title_panel.setBounds(0,0,800,100);
		
		button_panel.setLayout(new GridLayout(3,3));
		button_panel.setBackground(new Color(150,150,150));
		
		for (int i = 0; i < 9; i++) {
			buttons[i] = new JButton();
			button_panel.add(buttons[i]);
			buttons[i].setFont(new Font("MV Boli", Font.BOLD, 120));
			buttons[i].setFocusable(false);
			buttons[i].addActionListener(this);
			buttons[i].setBackground(new Color(214, 232, 255)); // Set to match your light blue style
		}
		generateRandomPowerUp();
		
		title_panel.add(textfield);
		frame.add(title_panel,BorderLayout.NORTH);
		frame.add(button_panel);
		JPanel control_panel = new JPanel();
		control_panel.setLayout(new GridLayout(1, 4));
		control_panel.add(undoButton);
		control_panel.add(redoButton);
		control_panel.add(powerUpButton);
		control_panel.add(restartButton); // Add this line

		frame.add(control_panel, BorderLayout.SOUTH);

		undoButton.addActionListener(this);
		redoButton.addActionListener(this);
		powerUpButton.addActionListener(this);
		powerUpButton.setEnabled(true);
		restartButton.addActionListener(this);
		
		firstTurn();

		
	}
	private static class CellState {
		String text;
		Color foreground;
		Color background;
	
		CellState(String text, Color foreground, Color background) {
			this.text = text;
			this.foreground = foreground;
			this.background = background;
		}
	}		

	private static class GameState {
		CellState[] board;
		LinkedList<Integer> moveOrder;
		boolean player1Turn;
	
		GameState(CellState[] board, LinkedList<Integer> moveOrder, boolean player1Turn) {
			this.board = board;
			this.moveOrder = new LinkedList<>(moveOrder); // deep copy để tránh mất dữ liệu
			this.player1Turn = player1Turn;
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == undoButton && !undoStack.isEmpty()) {
			if (!gameOver) redoStack.push(getGameState());
			setGameState(undoStack.pop());
			gameOver = false;
			return;
		}

		if (e.getSource() == redoButton && !redoStack.isEmpty()) {
			if (!gameOver) undoStack.push(getGameState());
			setGameState(redoStack.pop());
			gameOver = false;
			return;
		}

		if (e.getSource() == restartButton) {
			resetGame();
			return;
		}

		if (gameOver && moveOrder.size() < 9) return;

		if (e.getSource() == powerUpButton && gameOver) return;

		if (e.getSource() == powerUpButton && activePowerUp != null) {
			if (player1_turn != powerUpOwnedByPlayer1) return;

			if (activePowerUp == PowerUpType.EXTRA_TURN && extraTurnQueued) {
				textfield.setText("EXTRA TURN activated! " + (player1_turn ? "X" : "O") + " plays again!");
				extraTurnQueued = false;
				skipNextTurnSwitch = true;
				activePowerUp = null;
				powerUpButton.setText("Use Power-Up");
				powerUpButton.setEnabled(false);
				return;
			} else if (activePowerUp == PowerUpType.REMOVE_OPPONENT_MARK) {
				for (int i = 0; i < 9; i++) {
					if (!buttons[i].getText().equals("") &&
						!buttons[i].getText().equals(player1_turn ? "X" : "O")) {
						buttons[i].setText("");
						buttons[i].setForeground(Color.BLACK);
						buttons[i].setBackground(new Color(214, 232, 255));
						// ✅ Remove from moveOrder as well
						moveOrder.remove((Integer) i);

						// If power-up being removed was also currentPowerUp, reset it
						if (currentPowerUp != null &&
							currentPowerUp.position.equals(new java.awt.Point(i % 3, i / 3))) {
							currentPowerUp = null;
						}

						break;
					}
				}
			}
			activePowerUp = null;
			powerUpButton.setText("Use Power-Up");
			powerUpButton.setEnabled(false);
			return;
		}

		for (int i = 0; i < 9; i++) {
			if (e.getSource() == buttons[i]) {
				if (gameOver) return;

				// Nếu còn Power-Up chưa dùng → không được đánh tiếp
				if (activePowerUp != null && player1_turn == powerUpOwnedByPlayer1) {
					textfield.setText("Use your power-up first!");
					return;
				}

				// Nếu ô đã có đánh dấu và bàn đã đầy → cho phép ghi đè sau khi xóa ô cũ
				if (!buttons[i].getText().equals("")) {
					if (moveOrder.size() >= 9 && moveOrder.contains(i)) {
						// Remove oldest move
						int indexToRemove = -1;

						// Prefer removing opponent's mark first
						String currentSymbol = player1_turn ? "X" : "O";
						String opponentSymbol = player1_turn ? "O" : "X";

						for (int index : moveOrder) {
							if (buttons[index].getText().equals(opponentSymbol)) {
								indexToRemove = index;
								break;
							}
						}

						// If opponent mark not found, remove your own
						if (indexToRemove == -1) {
							indexToRemove = moveOrder.getFirst();
						}

						moveOrder.remove((Integer) indexToRemove);


						if (currentPowerUp != null &&
							currentPowerUp.position.equals(new java.awt.Point(indexToRemove % 3, indexToRemove / 3))) {
							currentPowerUp = null;
							activePowerUp = null;
							powerUpButton.setText("Use Power-Up");
							powerUpButton.setEnabled(false);
						}

						buttons[indexToRemove].setText("");
						buttons[indexToRemove].setForeground(Color.BLACK);
						buttons[indexToRemove].setBackground(new Color(214, 232, 255));
						buttons[indexToRemove].setToolTipText(null);
					} else {
						return;  // Cell is taken and board not full → disallow move
					}
				}

		
				// Nếu bàn đầy, cần xóa ô cũ NHƯNG làm việc này TRƯỚC khi lưu Undo
				if (moveOrder.size() >= 9) {
					int indexToRemove = -1;

				// Prefer removing opponent's mark first
				String currentSymbol = player1_turn ? "X" : "O";
				String opponentSymbol = player1_turn ? "O" : "X";

				for (int index : moveOrder) {
					if (buttons[index].getText().equals(opponentSymbol)) {
						indexToRemove = index;
						break;
					}
				}
			}

				// Bây giờ mới lưu trạng thái
				undoStack.push(getGameState());
				redoStack.clear();


				// Đánh dấu nước đi mới
				boolean currentPlayer = player1_turn;
				buttons[i].setText(currentPlayer ? "X" : "O");
				buttons[i].setForeground(currentPlayer ? new Color(255, 0, 0) : new Color(0, 0, 255));
				moveOrder.add(i);

												

				// Power-up trùng ô vừa đánh
				if (currentPowerUp != null &&
					currentPowerUp.position.equals(new java.awt.Point(i % 3, i / 3))) {
					buttons[i].setBackground(new Color(255, 255, 153));
					activePowerUp = currentPowerUp.type;
					if (activePowerUp == PowerUpType.EXTRA_TURN) {
						extraTurnQueued = true;
					}
					powerUpButton.setText("Use " + activePowerUp.name());
					powerUpButton.setEnabled(true);
					powerUpOwnedByPlayer1 = currentPlayer;

					String msg = (currentPlayer ? "X" : "O") + " received a power: " + activePowerUp.name();
					textfield.setText(msg);

					new javax.swing.Timer(2000, new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent evt) {
							if (!gameOver)
								textfield.setText((player1_turn ? "X" : "O") + " turn");
						}
					}).start();
					
					
					currentPowerUp = null;
				}

				check();
				
				// Đổi lượt (trừ khi dùng EXTRA_TURN)
				if (skipNextTurnSwitch) {
					skipNextTurnSwitch = false;
					if (!gameOver)
						textfield.setText((player1_turn ? "X" : "O") + " gets another turn!");
					// KHÔNG thay đổi player1_turn
				} else {
					player1_turn = !player1_turn;
					if (!gameOver)
						textfield.setText((player1_turn ? "X" : "O") + " turn");
				}

				if (!gameOver && currentPowerUp == null && Math.random() < 0.3) {
					generateRandomPowerUp();
				}
			}
		}
	}


	
	public void firstTurn() {
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(random.nextInt(2)==0) {
			player1_turn=true;
			textfield.setText("X turn");
		}
		else {
			player1_turn=false;
			textfield.setText("O turn");
		}
	}
	
	public void check() {
		//check X win conditions
		if(
				(buttons[0].getText().equals("X")) &&
				(buttons[1].getText().equals("X")) &&
				(buttons[2].getText().equals("X"))
				) {
			xWins(0,1,2);
		}
		if(
				(buttons[3].getText().equals("X")) &&
				(buttons[4].getText().equals("X")) &&
				(buttons[5].getText().equals("X"))
				) {
			xWins(3,4,5);
		}
		if(
				(buttons[6].getText().equals("X")) &&
				(buttons[7].getText().equals("X")) &&
				(buttons[8].getText().equals("X"))
				) {
			xWins(6,7,8);
		}
		if(
				(buttons[0].getText().equals("X")) &&
				(buttons[3].getText().equals("X")) &&
				(buttons[6].getText().equals("X"))
				) {
			xWins(0,3,6);
		}
		if(
				(buttons[1].getText().equals("X")) &&
				(buttons[4].getText().equals("X")) &&
				(buttons[7].getText().equals("X"))
				) {
			xWins(1,4,7);
		}
		if(
				(buttons[2].getText().equals("X")) &&
				(buttons[5].getText().equals("X")) &&
				(buttons[8].getText().equals("X"))
				) {
			xWins(2,5,8);
		}
		if(
				(buttons[0].getText().equals("X")) &&
				(buttons[4].getText().equals("X")) &&
				(buttons[8].getText().equals("X"))
				) {
			xWins(0,4,8);
		}
		if(
				(buttons[2].getText().equals("X")) &&
				(buttons[4].getText().equals("X")) &&
				(buttons[6].getText().equals("X"))
				) {
			xWins(2,4,6);
		}
		//check O win conditions
		if(
				(buttons[0].getText().equals("O")) &&
				(buttons[1].getText().equals("O")) &&
				(buttons[2].getText().equals("O"))
				) {
			oWins(0,1,2);
		}
		if(
				(buttons[3].getText().equals("O")) &&
				(buttons[4].getText().equals("O")) &&
				(buttons[5].getText().equals("O"))
				) {
			oWins(3,4,5);
		}
		if(
				(buttons[6].getText().equals("O")) &&
				(buttons[7].getText().equals("O")) &&
				(buttons[8].getText().equals("O"))
				) {
			oWins(6,7,8);
		}
		if(
				(buttons[0].getText().equals("O")) &&
				(buttons[3].getText().equals("O")) &&
				(buttons[6].getText().equals("O"))
				) {
			oWins(0,3,6);
		}
		if(
				(buttons[1].getText().equals("O")) &&
				(buttons[4].getText().equals("O")) &&
				(buttons[7].getText().equals("O"))
				) {
			oWins(1,4,7);
		}
		if(
				(buttons[2].getText().equals("O")) &&
				(buttons[5].getText().equals("O")) &&
				(buttons[8].getText().equals("O"))
				) {
			oWins(2,5,8);
		}
		if(
				(buttons[0].getText().equals("O")) &&
				(buttons[4].getText().equals("O")) &&
				(buttons[8].getText().equals("O"))
				) {
			oWins(0,4,8);
		}
		if(
				(buttons[2].getText().equals("O")) &&
				(buttons[4].getText().equals("O")) &&
				(buttons[6].getText().equals("O"))
				) {
			oWins(2,4,6);
		}
		// Nếu tất cả ô đã được đi và chưa ai thắng thì hòa
		boolean allFilled = true;
		for (JButton btn : buttons) {
			if (btn.getText().equals("")) {
				allFilled = false;
				break;
			}
		}
		if (allFilled && !gameOver) {
			textfield.setText("Draw!");
			gameOver = true;
		}

	}
	

	public void xWins(int a, int b, int c) {
		buttons[a].setBackground(Color.GREEN);
		buttons[b].setBackground(Color.GREEN);
		buttons[c].setBackground(Color.GREEN);
		textfield.setText("X wins");
		gameOver = true;
	}
	
	public void oWins(int a, int b, int c) {
		buttons[a].setBackground(Color.GREEN);
		buttons[b].setBackground(Color.GREEN);
		buttons[c].setBackground(Color.GREEN);
		textfield.setText("O wins");
		gameOver = true;
	}
	
	private GameState getGameState() {
		CellState[] state = new CellState[9];
		for (int i = 0; i < 9; i++) {
			state[i] = new CellState(
				buttons[i].getText(),
				buttons[i].getForeground(),
				buttons[i].getBackground()
			);
		}
		return new GameState(state, moveOrder, player1_turn);
	}
	

	private void setGameState(GameState state) {
		for (int i = 0; i < 9; i++) {
			buttons[i].setText(state.board[i].text);
			buttons[i].setForeground(state.board[i].foreground);
			buttons[i].setBackground(state.board[i].background);
		}
		moveOrder = new LinkedList<>(state.moveOrder);
		player1_turn = state.player1Turn;

		if (!gameOver)
		textfield.setText(player1_turn ? "X turn" : "O turn");
	}
	

	private void generateRandomPowerUp() {
		int index = random.nextInt(9);
		PowerUpType type = random.nextBoolean() ? PowerUpType.EXTRA_TURN : PowerUpType.REMOVE_OPPONENT_MARK;
		currentPowerUp = new PowerUp(type, new java.awt.Point(index % 3, index / 3));
	
		buttons[index].setToolTipText("Power-up: " + type.name());
	}				
		
	private void resetGame() {
		for (int i = 0; i < 9; i++) {
			buttons[i].setText("");
			buttons[i].setEnabled(true);
			buttons[i].setForeground(Color.BLACK);
			buttons[i].setBackground(new Color(214, 232, 255));
			buttons[i].setToolTipText(null);
		}
	
		moveOrder.clear();
		undoStack.clear();
		redoStack.clear();
		activePowerUp = null;
		currentPowerUp = null;
		extraTurnQueued = false;
		skipNextTurnSwitch = false;
		powerUpButton.setEnabled(false);
		powerUpButton.setText("Use Power-Up");
		gameOver = false;
	
		generateRandomPowerUp();
	
		// Đặt player1_turn trước khi gọi firstTurn để tránh mâu thuẫn
		player1_turn = true;
		firstTurn();
	}	
	
}
