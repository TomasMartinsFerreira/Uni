# 6.1
n <- 12
x <- 5.75
k_vals <- 0:floor(x)

terms <- sapply(k_vals, function(k) {
  (-1)^k * choose(n, k) * (x - k)^n
})

pn <- sum(terms) / factorial(n)

pn

# 6.2 a
media <- n*(0+1)/2
Variancia <-  n*(1-0)^2/12
pn_tlc <- pnorm(5.75, mean = media, sd = Variancia)
pn_tlc

# 6.2 b
set.seed(5457)
m <- 150

amostras <- matrix(runif(m * n, 0, 1), nrow = m, ncol = n,byrow = TRUE)
somas <- rowSums(amostras)

pn_sim <- mean(somas <= x)
pn_sim

# 6.3
desvio_absolutoTLC <- abs(pn - pn_tlc)
desvio_absolutoTLC

# 6.4
desvio_absolutoSIM <- abs(pn - pn_sim)
desvio_absolutoSIM

# 6.5
quociente <- desvio_absolutoTLC / desvio_absolutoSIM

quociente_arredondado <- round(quociente, 4)
quociente_arredondado