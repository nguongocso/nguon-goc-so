import AppRoutes from './routes/AppRoutes';
import { AuthProvider } from './contexts/AuthContext';
import { AppToaster } from '@/components/ui/toast';

function App() {
  return (
    <AuthProvider>
      <AppRoutes />
      <AppToaster />
    </AuthProvider>
  );
}

export default App;