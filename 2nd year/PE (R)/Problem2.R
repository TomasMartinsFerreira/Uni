library(ggplot2)
library(readxl)

wine_data <- read_excel("wine_prod_EU.xlsx")

wine_filtered <- subset(
  wine_data,
  !is.na(Category) & 
    `Product Group` != "Non-Vinified" & 
    Year == 2005
)

wine_filtered$Country_Group <- ifelse(
  wine_filtered$`Member State` %in% c("France", "Italy", "Spain", "Germany"),
  wine_filtered$`Member State`,
  "Others"
)

if (nrow(wine_filtered) == 0) {
  stop("Nenhum dado disponível após a filtragem. Verifique os critérios.")
}

ggplot(wine_filtered, aes(x = Category, y = Availability, fill = Country_Group)) +
  geom_bar(stat = "summary", fun = sum, position = position_dodge(width = 0.8)) +
  labs(
    title = "Availability of Wine by Category and Country (2005)",
    x = "Category of Wine",
    y = "Availability (10³ hL)",
    fill = "Country"
  ) +	
  theme_minimal() +
  theme(
    axis.text.x = element_text(angle = 45, hjust = 1, size = 10),
    legend.position = "top",
    plot.title = element_text(hjust = 0.5, face = "bold")
  ) +
  scale_fill_brewer(palette = "Set1")