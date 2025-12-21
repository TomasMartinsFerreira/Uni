set.seed(1420)

resultados <- replicate(41000, sum(sample(1:6, 3, replace = TRUE)))

freq_9 <- sum(resultados == 9) / 41000
freq_10 <- sum(resultados == 10) / 41000

diferenca <- round(freq_10 - freq_9, 4)
diferenca