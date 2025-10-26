import React from 'react';
import RepoForm from '../components/RepoForm';
import Container from '@mui/material/Container';
import Typography from '@mui/material/Typography';

type HomePageProps = {
  onAnalyze: (repoUrl: string) => void;
  isLoading: boolean;
};

const HomePage: React.FC<HomePageProps> = ({ onAnalyze, isLoading }) => {
  return (
    <Container maxWidth={false} sx={{ mt: 4, mb: 4 }}>
      <Typography variant="h3" component="h1" sx={{ fontWeight: 'bold', mb: 2 }}>
        CodeDocGen
      </Typography>
      <Typography variant="subtitle1" sx={{ mb: 4 }}>
        Enter a public Git repository URL to generate documentation and visualizations.
      </Typography>
      {/* @ts-ignore: RepoForm is a JS component, so ignore TS props check */}
      <RepoForm onAnalyze={onAnalyze} isLoading={isLoading} />
    </Container>
  );
};

export default HomePage; 
