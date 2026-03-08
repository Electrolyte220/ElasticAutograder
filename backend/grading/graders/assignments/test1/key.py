def add(a, b):
    def fib(n):
        if n <= 0:
            return 0
        elif n == 1:
            return 1
        else:
            return fib(n - 1) + fib(n - 2)
    
    # intentionally slow - runs fib on a large number
    fib(35)
    
    return a + b