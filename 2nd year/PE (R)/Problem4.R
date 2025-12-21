set.seed(1226)

lambda <- 22
k <- 8

expected_gamma <- lambda * gamma(1 + 1 / k)

sample <- rweibull(5500, shape = k, scale = lambda)
expected_mc <- mean(sample)

diff <- abs(expected_gamma - expected_mc)
round(diff, 4)