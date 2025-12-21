import pygame
import sys
import os

# --- CONFIG ---
FOLDER = "sample-nuruominoboards"
FILENAME = "test12.txt"  # Change this to any file name in the folder
CELL_SIZE = 50
GREY = (150, 150, 150)
WHITE = (255, 255, 255)
LINE_COLOR = (200, 200, 200)
REGION_BORDER_COLOR = (0, 0, 0)

# --- Load board from file ---
def load_board(filepath):
    with open(filepath, 'r') as file:
        return [
            list(map(int, line.strip().split('\t')))
            for line in file if line.strip()
        ]

# Full path to the board file
board_path = os.path.join(FOLDER, FILENAME)
board = load_board(board_path)

# Get dimensions
ROWS = len(board)
COLS = len(board[0])
WIDTH = COLS * CELL_SIZE
HEIGHT = ROWS * CELL_SIZE

# --- Init ---
pygame.init()
screen = pygame.display.set_mode((WIDTH, HEIGHT))
pygame.display.set_caption("LITS Puzzle From File")

# Click state
clicked = [[False for _ in range(COLS)] for _ in range(ROWS)]

# --- Main loop ---
running = True
while running:
    for event in pygame.event.get():
        if event.type == pygame.QUIT:
            running = False

        elif event.type == pygame.MOUSEBUTTONDOWN:
            x, y = pygame.mouse.get_pos()
            row = y // CELL_SIZE
            col = x // CELL_SIZE
            if 0 <= row < ROWS and 0 <= col < COLS:
                clicked[row][col] = not clicked[row][col]

    # Draw cells
    screen.fill(WHITE)
    for row in range(ROWS):
        for col in range(COLS):
            x = col * CELL_SIZE
            y = row * CELL_SIZE
            rect = pygame.Rect(x, y, CELL_SIZE, CELL_SIZE)
            color = GREY if clicked[row][col] else WHITE
            pygame.draw.rect(screen, color, rect)
            pygame.draw.rect(screen, LINE_COLOR, rect, 1)

            # Region borders
            current = board[row][col]
            if row == 0 or board[row - 1][col] != current:
                pygame.draw.line(screen, REGION_BORDER_COLOR, (x, y), (x + CELL_SIZE, y), 3)
            if row == ROWS - 1 or board[row + 1][col] != current:
                pygame.draw.line(screen, REGION_BORDER_COLOR, (x, y + CELL_SIZE), (x + CELL_SIZE, y + CELL_SIZE), 3)
            if col == 0 or board[row][col - 1] != current:
                pygame.draw.line(screen, REGION_BORDER_COLOR, (x, y), (x, y + CELL_SIZE), 3)
            if col == COLS - 1 or board[row][col + 1] != current:
                pygame.draw.line(screen, REGION_BORDER_COLOR, (x + CELL_SIZE, y), (x + CELL_SIZE, y + CELL_SIZE), 3)

    pygame.display.flip()

pygame.quit()
sys.exit()
