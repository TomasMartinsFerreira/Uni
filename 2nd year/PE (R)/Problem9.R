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
