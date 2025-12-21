# 4 --------------------------------------------------
set.seed(1226)

lambda <- 22
k <- 8

expected_gamma <- lambda * gamma(1 + 1 / k)

sample <- rweibull(5500, shape = k, scale = lambda)
expected_mc <- mean(sample)

diff <- abs(expected_gamma - expected_mc)
round(diff, 4)

# 5 --------------------------------------------------
set.seed(1420)

resultados <- replicate(41000, sum(sample(1:6, 3, replace = TRUE)))

freq_9 <- sum(resultados == 9) / 41000
freq_10 <- sum(resultados == 10) / 41000

diferenca <- round(freq_10 - freq_9, 4)
diferenca

# 6 --------------------------------------------------
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

amostras <- matrix(runif(m * n, 0, 1), nrow = m, ncol = n)
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

# 7 --------------------------------------------------
n <- 11
soma_x <- 53.7
soma_logx <- 17.39
media_x <- soma_x / n
media_logx <- soma_logx / n

const <- log(media_x) - media_logx

f <- function(alpha) {
  log(alpha) - digamma(alpha) - const
}

alpha_hat <- uniroot(f, interval = c(1.3, 108.7))$root

lambda_hat <- alpha_hat / media_x

modo <- (alpha_hat - 1) / lambda_hat

round(modo, 2)

# 8 --------------------------------------------------
set.seed(1036)

m <- 1700
n <- 12
mu <- 0.1      
sigma <- 1.4
gamma <- 0.91

alpha <- 1 - gamma


z <- qnorm(1 - alpha/2)

amostras <- matrix(rnorm(m * n, mean = mu, sd = sigma), nrow = m, ncol = n)

medias <- rowMeans(amostras)

erro_padrao <- sigma / sqrt(n)

lim_inf <- medias - z * erro_padrao
lim_sup <- medias + z * erro_padrao

contem_mu <- (lim_inf <= mu) & (mu <= lim_sup)

proporcao_contem <- mean(contem_mu)

quociente <- round(proporcao_contem / gamma,4) 
quociente
# 9 -------------------------------------------------------------
set.seed(2084)
m <- 1000
n <- 27
mu0 <- 2.0
mu1 <- 2.2
alpha <- 0.1

crit_value <- qchisq(1 - alpha, df = 2 * n)

rejections <- 0

for (i in 1:m) {
  sample <- rexp(n, rate = 1 / mu1)
  x_bar <- mean(sample)
  T0 <- 2 * n * x_bar / mu0
  if (T0 > crit_value) {
    rejections <- rejections + 1
  }
}

beta_hat <- 1 - (rejections / m)

beta_theoretical <- pchisq(crit_value * (mu0 / mu1), df = 2 * n)

quociente <- round(beta_hat / beta_theoretical,4)
quociente

#10 ---------------------------------------------------------
set.seed(3675)
dados <- c(4.1, 2.5, 2.2, 3.3, 0.9, 1.9, 1.9, 2.8, 1.6, 5.3, 2.1, 3.9, 4.2, 2.6, 3.1, 1, 2, 6.5, 2.5, 3.8,
           0.6, 1.9, 2.4, 2.3, 3.7, 4.5, 2.7, 1.5, 1.4, 2.2, 5.5, 5.5, 0.6, 3.3, 3.7, 4, 0.4, 3.7, 0.8, 2,
           0.2, 0.2, 6.1, 2.5, 3.5, 3.8, 2.5, 2.1, 4.1, 2.3, 0.4, 1, 3.4, 2.6, 2.1, 2.5, 4.3, 0.8, 2.8,
           1.3, 3.8, 0.9, 4.1, 0.8, 1.1, 2.9, 2.3, 3.9, 1.5, 2.1, 4, 3.2, 2.4, 1, 3.7, 2.5, 0.7, 4.1, 0.8,
           4.5, 3.2, 4, 4.9, 5.5, 3.4, 2.2, 1.6, 6.3, 5.3, 1.6, 1.1, 1.8, 5.2, 1.9, 3.3, 2.3, 3.8, 3.4,
           1.4, 4.3, 2.8, 3.2, 0.7, 1.2, 5.4, 0.5, 4.5, 0.2, 1.9, 2.4, 5.9, 0.8, 2.8, 5.3, 5.6, 3.9, 1.1,
           3.7, 0.8, 2.1, 1.8, 0.6, 1.2, 0.9, 0.9, 2.8, 2.1, 2.3, 3.2, 3.3, 0.7, 0.2, 1.1, 2.2, 4.6, 0.8,
           0.9, 3.4, 1.3, 2.9, 3.5, 2.2, 2.3, 2.5, 1.7, 2.2, 2.5, 5.1, 2.4, 2.5, 1, 2.4, 1.8, 3.9, 2.2, 6,
           1.4, 2.4, 1.4, 2.8, 2.3, 2.2, 5.2, 4.1, 2, 3.4, 3.6, 1.2, 1.5, 3.8, 0.7, 0.1, 1.1, 1.3, 2.2,
           3.2, 3.3, 2.9, 1.7, 0.8, 2, 2.9, 3.5, 1.8, 2.8, 4.3, 2.8, 2.8, 6, 4.3, 2.6, 3.4, 1.6, 3.7, 3.9,
           3, 1.1, 3, 3.6, 3.1, 1.4, 2.2, 1.8, 2.1, 4.1, 4.3, 2.6, 1.4, 3.2, 2.6, 2.1, 1.3, 2, 1.6, 1.5,
           1.6, 1, 5.6, 3.3, 4.5)

subamostra <- sample(dados, 175, replace = FALSE)

sigma <- 2.1

probs <- seq(0, 1, length.out = 8)
limites <- sigma * sqrt(-2 * log(1 - probs[-1]))

limites <- c(0, limites)

observadas <- hist(subamostra, breaks = limites, plot = FALSE)$counts

esperadas <- rep(175 / 7, 7)

estatistica <- sum((observadas - esperadas)^2 / esperadas)
valor_p <- pchisq(estatistica, df = 6, lower.tail = FALSE)
round(valor_p,4)

