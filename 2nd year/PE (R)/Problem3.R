library(ggplot2)
library(dplyr)

clima <- read.csv("clima.csv")

clima$date <- as.POSIXct(clima$Data, format = "%Y-%m-%d %H:%M:%S")

clima$year <- as.numeric(format(clima$date, "%Y"))
clima$month <- as.numeric(format(clima$date, "%m"))
clima$day <- as.numeric(format(clima$date, "%d"))

julho_2010 <- clima %>% 
  filter(year == 2010 & month == 7)

mediana_diaria <- julho_2010 %>%
  group_by(day) %>%
  summarise(mediana_orvalho = median(Orvalho, na.rm = TRUE))

ggplot() +
  geom_line(data = julho_2010, 
            aes(x = date, y = Orvalho, group = day), 
            color = "black", alpha = 1) +
  
  geom_line(data = mediana_diaria, 
            aes(x = as.POSIXct(paste("2010-07", day, sep="-")), 
                y = mediana_orvalho), 
            color = "red", size = 1) +
  
  labs(title = "Variação Horária do Orvalho em Julho de 2010",
       subtitle = "Linhas cinzas: medições | Linha vermelha: mediana diária",
       x = "Data",
       y = "Orvalho (°C)") +
  
  scale_x_datetime(date_labels = "%d/%b", date_breaks = "5 days") +
  
  theme_minimal()