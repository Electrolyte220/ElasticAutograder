import time
def twoSum(nums, target):
    time.sleep(5)
    seen = {}

    for i, num in enumerate(nums):
        need = target - num
        if need in seen:
            return [seen[need], i]
        seen[num] = i

    return []
