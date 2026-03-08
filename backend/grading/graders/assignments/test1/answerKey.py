FUNCTION_NAME = "fizzbuzz"

TEST_CASES = [
    {"input": (5,),  "expected": ["1", "2", "Fizz", "4", "Buzz"]},
    {"input": (1,),  "expected": ["1"]},
    {"input": (3,),  "expected": ["1", "2", "Fizz"]},
    {"input": (15,), "expected": ["1", "2", "Fizz", "4", "Buzz", "Fizz", "7", "8", "Fizz", "Buzz", "11", "Fizz", "13", "14", "FizzBuzz"]},
    {"input": (6,),  "expected": ["1", "2", "Fizz", "4", "Buzz", "Fizz"]},
    {"input": (10,), "expected": ["1", "2", "Fizz", "4", "Buzz", "Fizz", "7", "8", "Fizz", "Buzz"]},
]