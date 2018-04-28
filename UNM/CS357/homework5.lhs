\begin{code}
import Data.List

--5.1 Trees
data Tree a = E
            | T a (Tree a) (Tree a)
            deriving (Eq, Show)

bfnum :: Tree a -> Tree Int
bfnum = undefined

--5.2 Expression Trees
type Identifier = String

data Expr = Num Integer
          | Var Identifier
          | Let {var :: Identifier, value :: Expr, body :: Expr}
          | Add Expr Expr
          | Sub Expr Expr
          | Mul Expr Expr
          | Div Expr Expr
          deriving (Eq)

type Env = Identifier -> Integer

emptyEnv :: Env
emptyEnv = \s -> error ("unbound: " ++ s)

extendEnv :: Env -> Identifier -> Integer -> Env
extendEnv oldEnv s n s' = if s' == s then n else oldEnv s'

evalInEnv :: Env -> Expr -> Integer
evalInEnv = undefined

--5.3 Infinite Lists
diag :: [[a]] -> [a]
diag = undefined
\end{code}
