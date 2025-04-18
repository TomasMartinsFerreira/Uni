// Target class (position and width)
class Target {
  constructor(x, y, w, l, id) {
    this.x = x;
    this.y = y;
    this.width = w;
    this.label = l;
    this.id = id;
    this.highlight = false;
    this.isClicked = false;
  }

  // Checks if a mouse click took place within the target
  clicked(mouse_x, mouse_y) {
    return dist(this.x, this.y, mouse_x, mouse_y) < this.width / 2;
  }

  // Function to map the first letter to a smooth, distinct color
  getCircleColor() {
    let firstLetter = this.label.charAt(0).toUpperCase(); // Get first letter and convert to uppercase

    // Smoother, balanced colors without yellow or gold-like tones
    const colorMap = {
      A: "#A5D6A7", // Light Green
      B: "#FF8A65", // Soft Coral
      C: "#80DEEA", // Light Teal
      D: "#81C784", // Light Olive Green
      E: "#4FC3F7", // Light Cyan
      F: "#90CAF9", // Light Blue
      G: "#43FFAB", // Soft Orange
      H: "#C4A2FF", // Lavender
      I: "#FF8A65", // Warm Brown
      J: "#66BB6A", // Fresh Green
      K: "#81D4FA", // Soft Blue
      L: "#80CBC4", // Mint Green
      M: "#A0F040", // Olive Green
      N: "#89B7FF", // Deep Orange
      O: "#EC7FFF", // Soft Purple
      P: "#26C6DA", // Aqua Blue
      Q: "#FFA4C3", // Bright Pink
      R: "#9ACEFF", // Deep Purple
      S: "#FFC6F5", // Vibrant Pink
      T: "#00C853", // Fresh Lime Green
      U: "#7B1FA2", // Deep Violet
      V: "#F57C00", // Warm Orange
      W: "#009688", // Teal
      X: "#00ACC1", // Bright Cyan
      Y: "#66BB6A", // Soft Green
      Z: "#9E9D24", // Olive
    };

    // Return the mapped color or default to a smooth color if not mapped
    return colorMap[firstLetter] || "#90A4AE"; // Default to muted teal
  }

  // Draws the target (i.e., a circle) and its label
  draw() {
    // Get the circle color based on the first letter
    let circleColor = this.getCircleColor();

    // Makes it change color for a second to give feedback to the user
    // If clicked and 100ms haven't passed, change color
    if (this.isClicked && millis() - clickTime < 100){
      fill("#FF47A9");
    } else {
      fill(circleColor); // Back to Default Color
      this.isClicked = false; // Reset after 100ms
    }
    
    stroke("white");
    strokeWeight(2)
    circle(this.x, this.y, this.width);

    // Split the label into the first letter and the rest
    let firstLetter = this.label.charAt(0);
    let restOfWord = this.label.substring(1);

    // Set text alignment and font sizes
    let firstLetterSize = 28; // Bigger for emphasis
    let highlightFirstLetterSize = firstLetterSize + 10
    let restTextSize = 19; // Slightly bigger for the rest of the word
    
    if (this.highlight){
      textFont("Arial", highlightFirstLetterSize);
    } else {
      textFont("Arial", firstLetterSize);
    }
    
    textStyle(BOLD); // Make first letter bold
    let firstLetterWidth = textWidth(firstLetter);

    textFont("Arial", restTextSize);
    textStyle(NORMAL); // Keep rest of the word normal weight
    let restWordWidth = textWidth(restOfWord);

    // Compute positions
    let totalWidth = firstLetterWidth + restWordWidth;
    let centerX = this.x - totalWidth / 2;

    // Draw first letter (Bold and Gold)
    strokeWeight(2);
    stroke("black");
    textAlign(LEFT, CENTER);
    textStyle(BOLD);
    
    // Split the text into words
    let words = split(restOfWord, ' ');
    let lineHeight = 20;
    
    if (words.length > 1){
      centerX = this.x - totalWidth / 4;
    }
    
    if (this.highlight){
      textFont("Arial", highlightFirstLetterSize);
      fill(color(255, 85, 0));
      text(firstLetter, centerX - firstLetterWidth * 0.8, this.y); // Shift further left
    } else {
      textFont("Arial", firstLetterSize);
      fill("#FFC107"); // Smooth Gold     
      text(firstLetter, centerX - firstLetterWidth * 0.8, this.y); // Shift further left
    }

    // Draw rest of the word (slightly bigger and dark charcoal)
    strokeWeight(0);
    textFont("Arial", restTextSize);
    textStyle(BOLD);
    fill("#263238"); // Dark charcoal
    textAlign(LEFT, CENTER);
    
    // para labels com mais de 3 palavras não queremos que elas cresçam muito para baixo
    let groupedWords = [];
    if (words.length > 3){
      for (let i = 0; i < words.length; i += 2) {
        // junta a palavra atual com a próxima (se existir)
        let group = words[i] + (words[i + 1] ? " " + words[i + 1] : "");
        groupedWords.push(group);
      }
      words = groupedWords;
    }
    
    // desenha as palavras, aplicando um line break sempre que for preciso
    for (let i = 0; i < words.length; i++){
      text(words[i], centerX + firstLetterWidth * 0.3, this.y + lineHeight * i);
    }
    
    textStyle(NORMAL); // volta ao normal para não afetar outros elementos de texto
  }
}
