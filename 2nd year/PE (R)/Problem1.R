library(ggplot2)

dados <- read.csv("winequality-white-q5.csv")

ggplot(dados, aes(x = factor(quality), y = sqrt(citric.acid))) +
  geom_boxplot(
    outlier.colour = "red",
    outlier.size = 1.5,
    fill = "lightblue",
    alpha = 1,
  ) +
  labs(
    x = "Wine Quality",
    y = "sqrt(citric.acid)",
  ) +
  theme_minimal()