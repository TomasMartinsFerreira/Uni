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