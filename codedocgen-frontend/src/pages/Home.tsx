import React from 'react';
import RepoForm from '../components/RepoForm';

type HomePageProps = {
  onAnalyze: (repoUrl: string) => void;
  isLoading: boolean;
};

const HomePage: React.FC<HomePageProps> = ({ onAnalyze, isLoading }) => {
  return (
    <div className="container mx-auto p-4">
      <h1 className="text-3xl font-bold mb-4">CodeDocGen</h1>
      <p className="mb-8">Enter a public Git repository URL to generate documentation and visualizations.</p>
      {/* @ts-ignore: RepoForm is a JS component, so ignore TS props check */}
      <RepoForm onAnalyze={onAnalyze} isLoading={isLoading} />
    </div>
  );
};

export default HomePage; 