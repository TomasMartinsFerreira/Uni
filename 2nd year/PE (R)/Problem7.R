# Dados
n <- 11
soma_x <- 53.7
soma_logx <- 17.39
media_x <- soma_x / n
media_logx <- soma_logx / n

# Constante para resolver a equação
const <- log(media_x) - media_logx

# Função a ser zerada
f <- function(alpha) {
  log(alpha) - digamma(alpha) - const
}

# Encontrar raiz com uniroot
alpha_hat <- uniroot(f, interval = c(1.3, 108.7))$root

# Calcular lambda_hat
lambda_hat <- alpha_hat / media_x

# Calcular modo
modo <- (alpha_hat - 1) / lambda_hat

# Resultado final arredondado
round(modo, 2)
